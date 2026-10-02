package com.financialgps.domain.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Portfolio aggregation + DTI + portfolio payoff (spec §5.4, §6, oracle §11.2 REF-P01–REF-P04).
 * Blocker propagation (invariant §12.8): ANY blocked active debt forces the portfolio projection
 * to BLOCKED with null date/months/interest and reason PORTFOLIO_CONTAINS_BLOCKED_DEBTS.
 */
public record DebtSummaryResult(
        BigDecimal totalOutstandingDebt,
        BigDecimal totalMinimumMonthlyPayment,
        BigDecimal totalPlannedMonthlyPayment,
        BigDecimal totalMonthlyAccruedInterest,
        String currency,
        DtiResult debtToIncome,
        PortfolioProjectionResult portfolioProjection,
        int blockedDebtCount,
        LocalDate asOf) {

    public DebtSummaryResult {
        Objects.requireNonNull(totalOutstandingDebt, "totalOutstandingDebt");
        Objects.requireNonNull(totalMinimumMonthlyPayment, "totalMinimumMonthlyPayment");
        Objects.requireNonNull(totalPlannedMonthlyPayment, "totalPlannedMonthlyPayment");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(debtToIncome, "debtToIncome");
        Objects.requireNonNull(portfolioProjection, "portfolioProjection");
        Objects.requireNonNull(asOf, "asOf");
    }

    public record DtiResult(String status, BigDecimal ratio, String reasonCode, String explanation) {
        public DtiResult {
            Objects.requireNonNull(status, "status");
        }
    }

    public record PortfolioProjectionResult(ProjectionStatus status,
                                            LocalDate projectedDebtFreeDate,
                                            Integer totalMonthsRemaining,
                                            BigDecimal totalInterestRemaining,
                                            String reasonCode,
                                            String explanation,
                                            List<BlockedDebt> blockedDebts) {
        public PortfolioProjectionResult {
            Objects.requireNonNull(status, "status");
            blockedDebts = blockedDebts == null ? List.of() : List.copyOf(blockedDebts);
        }
    }

    public record BlockedDebt(String creditor, String reasonCode, String explanation) {
        public BlockedDebt {
            Objects.requireNonNull(creditor, "creditor");
            Objects.requireNonNull(reasonCode, "reasonCode");
            Objects.requireNonNull(explanation, "explanation");
        }
    }
}
