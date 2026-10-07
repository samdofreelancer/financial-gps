package com.financialgps.domain.debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure portfolio aggregation (spec §5.4 + §6). ARCHIVED debts are excluded by the caller contract
 * (application/infrastructure filter); PAID_OFF debts contribute nothing but keep history visible.
 */
public final class DebtSummaryCalculator {

    private DebtSummaryCalculator() {
    }

    public static DebtSummaryResult summarize(List<Debt> debts,
                                              BigDecimal totalMonthlyIncome,
                                              String currency,
                                              LocalDate asOf,
                                              DebtCalculationPolicy policy) {
        Objects.requireNonNull(debts, "debts");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(policy, "policy");

        List<Debt> active = new ArrayList<>();
        for (Debt debt : debts) {
            if (debt.contributesToTotals()) {
                active.add(debt);
            }
        }

        BigDecimal totalBalance = BigDecimal.ZERO.setScale(2);
        BigDecimal totalMin = BigDecimal.ZERO.setScale(2);
        BigDecimal totalPlanned = BigDecimal.ZERO.setScale(2);
        for (Debt debt : active) {
            totalBalance = totalBalance.add(debt.outstandingBalance().amount());
            totalMin = totalMin.add(debt.minimumPayment().amount());
            totalPlanned = totalPlanned.add(debt.plannedPayment().amount());
        }

        // Accrued interest for the month, taken from the same projection the blockers compare the
        // payment against. Null when any active debt has an unknown rate: a partial sum would
        // understate the burden, so the UI must say "unknown" instead of a reassuring number.
        BigDecimal totalAccrued = null;
        for (Debt debt : active) {
            BigDecimal interest = DebtPayoffCalculator.project(debt, asOf, policy).monthlyInterest();
            if (interest == null) {
                totalAccrued = null;
                break;
            }
            totalAccrued = (totalAccrued == null ? BigDecimal.ZERO.setScale(2) : totalAccrued)
                    .add(interest);
        }

        DebtSummaryResult.DtiResult dti = debtToIncome(totalMin, totalMonthlyIncome, currency, policy);
        DebtSummaryResult.PortfolioProjectionResult projection = portfolioProjection(active, asOf, policy);
        int blockedCount = (int) projection.blockedDebts().size();

        return new DebtSummaryResult(totalBalance, totalMin, totalPlanned, totalAccrued, currency, dti,
                projection, blockedCount, asOf);
    }

    private static DebtSummaryResult.DtiResult debtToIncome(BigDecimal totalMin,
                                                            BigDecimal income,
                                                            String currency,
                                                            DebtCalculationPolicy policy) {
        if (income == null || income.signum() <= 0) {
            return new DebtSummaryResult.DtiResult("UNAVAILABLE", null, "ZERO_OR_MISSING_INCOME",
                    "Debt-to-income is unavailable: monthly income is zero or the profile is not set.");
        }
        if (totalMin.signum() == 0) {
            return new DebtSummaryResult.DtiResult("AVAILABLE",
                    BigDecimal.ZERO.setScale(4), null, null);
        }
        BigDecimal ratio = totalMin.divide(income, 4, policy.roundingMode());
        return new DebtSummaryResult.DtiResult("AVAILABLE", ratio, null,
                "Total minimum monthly debt (" + totalMin.toPlainString() + " " + currency
                        + ") divided by monthly income (" + income.toPlainString() + " " + currency + ")");
    }

    private static DebtSummaryResult.PortfolioProjectionResult portfolioProjection(
            List<Debt> active, LocalDate asOf, DebtCalculationPolicy policy) {
        if (active.isEmpty()) {
            return new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.COMPLETED, asOf,
                    0, BigDecimal.ZERO.setScale(2), "DEBT_ALREADY_PAID",
                    "No active debts: the portfolio is already debt-free.", List.of());
        }
        List<DebtSummaryResult.BlockedDebt> blocked = new ArrayList<>();
        LocalDate latest = null;
        int maxMonths = 0;
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(2);
        for (Debt debt : active) {
            DebtProjectionResult projection = DebtPayoffCalculator.project(debt, asOf, policy);
            if (projection.status() == ProjectionStatus.BLOCKED) {
                blocked.add(new DebtSummaryResult.BlockedDebt(debt.creditor(),
                        projection.reasonCode(), projection.explanation()));
            } else {
                if (latest == null || projection.projectedPayoffDate().isAfter(latest)) {
                    latest = projection.projectedPayoffDate();
                }
                maxMonths = Math.max(maxMonths, projection.numberOfPayments());
                totalInterest = totalInterest.add(projection.totalInterest());
            }
        }
        if (!blocked.isEmpty()) {
            return new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.BLOCKED, null,
                    null, null, "PORTFOLIO_CONTAINS_BLOCKED_DEBTS",
                    "At least one active debt cannot be projected to payoff; "
                            + "the portfolio debt-free date is unavailable.",
                    blocked);
        }
        return new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.AVAILABLE, latest,
                maxMonths, totalInterest, null,
                "All active debts will be paid off by " + latest + " under planned payments.",
                List.of());
    }
}
