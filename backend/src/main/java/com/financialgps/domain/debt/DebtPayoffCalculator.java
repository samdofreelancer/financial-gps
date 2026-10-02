package com.financialgps.domain.debt;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Deterministic monthly simple amortization (spec §5.2, oracle §11.1 REF-D01–REF-D09).
 *
 * <p>Per month: {@code interest = round(balance * rate / 12)}; if the payment cannot cover that
 * interest the projection is BLOCKED before the loop (no phantom dates). Otherwise the balance
 * declines; the final payment is clamped to {@code balance + interest} so the balance never goes
 * negative and no over-credit is granted.
 */
public final class DebtPayoffCalculator {

    private DebtPayoffCalculator() {
    }

    public static DebtProjectionResult project(Debt debt, LocalDate asOf, DebtCalculationPolicy policy) {
        Objects.requireNonNull(debt, "debt");
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(policy, "policy");

        if (debt.outstandingBalance().amount().signum() == 0) {
            return DebtProjectionResult.completed(asOf);
        }
        if (debt.annualInterestRate() == null) {
            // monthlyInterest stays null: an unknown rate must never be presented as 0%.
            return DebtProjectionResult.blocked("INTEREST_RATE_MISSING",
                    "Annual interest rate is missing. Enter the contractual rate to project payoff; "
                            + "the engine never assumes 0%.", null);
        }

        BigDecimal monthlyInterest = monthlyInterest(debt, policy);
        int comparison = debt.plannedPayment().amount().compareTo(monthlyInterest);
        if (comparison < 0) {
            return DebtProjectionResult.blocked("PAYMENT_DOES_NOT_COVER_INTEREST",
                    "Monthly planned payment (" + debt.plannedPayment().asDecimalString()
                            + ") is less than monthly accrued interest ("
                            + monthlyInterest.toPlainString() + "). Balance will grow.",
                    monthlyInterest);
        }
        if (comparison == 0) {
            return DebtProjectionResult.blocked("PAYMENT_COVERS_ONLY_INTEREST",
                    "Monthly planned payment only covers monthly interest ("
                            + monthlyInterest.toPlainString() + "). Principal will never decrease.",
                    monthlyInterest);
        }

        BigDecimal balance = debt.outstandingBalance().amount();
        BigDecimal payment = debt.plannedPayment().amount();
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(policy.monetaryScale());
        for (int month = 1; month <= policy.maxSimulationMonths(); month++) {
            BigDecimal interest = interestOn(balance, debt, policy);
            totalInterest = totalInterest.add(interest);
            BigDecimal finalPayment = balance.add(interest);
            if (payment.compareTo(finalPayment) >= 0) {
                return DebtProjectionResult.available(
                        asOf.plusMonths(month), month, totalInterest, finalPayment, monthlyInterest);
            }
            balance = balance.add(interest).subtract(payment);
        }
        return DebtProjectionResult.blocked("PAYOFF_HORIZON_EXCEEDS_MAXIMUM",
                "Payoff horizon exceeds the computational safety limit ("
                        + policy.maxSimulationMonths()
                        + " months). This guards the simulation engine; it does not invalidate the loan.",
                monthlyInterest);
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
