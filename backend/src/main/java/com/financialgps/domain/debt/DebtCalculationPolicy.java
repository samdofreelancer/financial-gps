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
        // 360 months = 30 years of runway (spec 002 §5.1/§5.3): covers standard mortgages while
        // staying a cheap computational guardrail — worst case is 360 primitive-decimal
        // iterations. Anything beyond reports PAYOFF_HORIZON_EXCEEDS_MAXIMUM, never an
        // unbounded loop.
        return new DebtCalculationPolicy(
                PaymentFrequency.MONTHLY, 360, 2, RoundingMode.HALF_UP);
    }
}
