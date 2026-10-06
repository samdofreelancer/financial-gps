package com.financialgps.domain.debt;

import java.math.RoundingMode;

/**
 * Explicit calculation policy (spec §5.1). Every payoff projection executes against this policy;
 * nothing is an ambient default. {@code maxSimulationMonths} is strictly a computational safety
 * guardrail, not a business rule invalidating long-term loans.
 */
public record DebtCalculationPolicy(
        PaymentFrequency paymentFrequency,
        int maxSimulationMonths,
        int monetaryScale,
        RoundingMode roundingMode) {

    public enum PaymentFrequency {
        MONTHLY
    }

    public DebtCalculationPolicy {
        if (paymentFrequency == null) {
            throw new IllegalArgumentException("paymentFrequency is required");
        }
        if (maxSimulationMonths <= 0) {
            throw new IllegalArgumentException("maxSimulationMonths must be positive");
        }
        if (monetaryScale < 0) {
            throw new IllegalArgumentException("monetaryScale must not be negative");
        }
        if (roundingMode == null) {
            throw new IllegalArgumentException("roundingMode is required");
        }
    }

    public static DebtCalculationPolicy defaults() {
        // 720 months = 60 years of runway: covers long mortgages (30–50y) while staying a cheap
        // computational guardrail — worst case is 720 primitive-decimal iterations.
        return new DebtCalculationPolicy(
                PaymentFrequency.MONTHLY, 720, 2, RoundingMode.HALF_UP);
    }
}
