package com.financialgps.domain.debt;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic monthly simple amortization (spec §5.2, oracle §11.1 REF-D01–REF-D09).
 *
 * <p>Per month: {@code interest = round(balance * rate / 12)}; if the payment cannot cover that
 * interest the projection is BLOCKED before the loop (no phantom dates). Otherwise the balance
 * declines; the final payment is clamped to {@code balance + interest} so the balance never goes
 * negative and no over-credit is granted.
 *
 * <p>{@link #schedule} is the single loop: it yields the per-period rows (payment date, principal,
 * interest, ending balance) and the projection derived from them, so the payoff ETA and the payment
 * calendar can never disagree. {@link #project} is the projection-only view of the same loop.
 */
public final class DebtPayoffCalculator {

    private DebtPayoffCalculator() {
    }

    public static DebtProjectionResult project(Debt debt, LocalDate asOf, DebtCalculationPolicy policy) {
        return schedule(debt, asOf, policy).projection();
    }

    /**
     * Full amortization calendar. Row dates honour {@code debt.dueDay()} (clamped to the month's
     * length); the projection's payoff date keeps the oracle rule {@code asOf.plusMonths(n)}
     * (REF-D01–REF-D09), so with an unknown due day both are the same date.
     */
    public static DebtScheduleResult schedule(Debt debt, LocalDate asOf,
                                                                DebtCalculationPolicy policy) {
        Objects.requireNonNull(debt, "debt");
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(policy, "policy");

        if (debt.outstandingBalance().amount().signum() == 0) {
            return new DebtScheduleResult(DebtProjectionResult.completed(asOf), List.of());
        }
        if (debt.annualInterestRate() == null) {
            // monthlyInterest stays null: an unknown rate must never be presented as 0%.
            return new DebtScheduleResult(DebtProjectionResult.blocked("INTEREST_RATE_MISSING",
                    "Annual interest rate is missing. Enter the contractual rate to project payoff; "
                            + "the engine never assumes 0%.", null), List.of());
        }

        BigDecimal monthlyInterest = monthlyInterest(debt, policy);
        int comparison = debt.plannedPayment().amount().compareTo(monthlyInterest);
        if (comparison < 0) {
            return new DebtScheduleResult(DebtProjectionResult.blocked(
                    "PAYMENT_DOES_NOT_COVER_INTEREST",
                    "Monthly planned payment (" + debt.plannedPayment().asDecimalString()
                            + ") is less than monthly accrued interest ("
                            + monthlyInterest.toPlainString() + "). Balance will grow.",
                    monthlyInterest), List.of());
        }
        if (comparison == 0) {
            return new DebtScheduleResult(DebtProjectionResult.blocked(
                    "PAYMENT_COVERS_ONLY_INTEREST",
                    "Monthly planned payment only covers monthly interest ("
                            + monthlyInterest.toPlainString() + "). Principal will never decrease.",
                    monthlyInterest), List.of());
        }

        BigDecimal balance = debt.outstandingBalance().amount();
        BigDecimal payment = debt.plannedPayment().amount();
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(policy.monetaryScale());
        List<DebtScheduleEntry> rows = new ArrayList<>();
        for (int month = 1; month <= policy.maxSimulationMonths(); month++) {
            BigDecimal interest = interestOn(balance, debt, policy);
            totalInterest = totalInterest.add(interest);
            LocalDate dueDate = dueDate(asOf, month, debt.dueDay());
            BigDecimal finalPayment = balance.add(interest);
            if (payment.compareTo(finalPayment) >= 0) {
                rows.add(new DebtScheduleEntry(month, dueDate, finalPayment, balance, interest,
                        BigDecimal.ZERO.setScale(policy.monetaryScale())));
                DebtProjectionResult projection = DebtProjectionResult.available(
                        asOf.plusMonths(month), month, totalInterest, finalPayment, monthlyInterest);
                return new DebtScheduleResult(projection, rows);
            }
            BigDecimal principal = payment.subtract(interest);
            balance = balance.add(interest).subtract(payment);
            rows.add(new DebtScheduleEntry(month, dueDate, payment, principal, interest, balance));
        }
        return new DebtScheduleResult(DebtProjectionResult.blocked(
                "PAYOFF_HORIZON_EXCEEDS_MAXIMUM",
                "Payoff horizon exceeds the computational safety limit ("
                        + policy.maxSimulationMonths()
                        + " months). This guards the simulation engine; it does not invalidate the loan.",
                monthlyInterest), List.of());
    }

    /** Due date of period {@code month}: {@code asOf + month}, on the contractual day when known. */
    private static LocalDate dueDate(LocalDate asOf, int month, Integer dueDay) {
        LocalDate base = asOf.plusMonths(month);
        if (dueDay == null) {
            return base;
        }
        return base.withDayOfMonth(Math.min(dueDay, base.lengthOfMonth()));
    }

    private static BigDecimal monthlyInterest(Debt debt, DebtCalculationPolicy policy) {
        return interestOn(debt.outstandingBalance().amount(), debt, policy);
    }

    private static BigDecimal interestOn(BigDecimal balance, Debt debt, DebtCalculationPolicy policy) {
        if (debt.annualInterestRate().isZero()) {
            return BigDecimal.ZERO.setScale(policy.monetaryScale());
        }
        return balance.multiply(debt.annualInterestRate().value())
                .divide(BigDecimal.valueOf(12), policy.monetaryScale(), RoundingMode.HALF_UP);
    }

    /** Currency for derived interest/total amounts: the debt's own currency. */
    public static Money interestMoney(Debt debt, BigDecimal interest) {
        return Money.of(interest.toPlainString(), debt.outstandingBalance().currency());
    }
}
