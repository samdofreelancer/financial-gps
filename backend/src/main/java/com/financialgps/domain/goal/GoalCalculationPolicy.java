package com.financialgps.domain.goal;

import java.math.RoundingMode;

/** Explicit calculation policy (spec §4.3, §5). Monthly cadence, never understated capacity. */
public record GoalCalculationPolicy(
        PaymentFrequency paymentFrequency,
        int monetaryScale,
        int ratioScale,
        RoundingMode roundingMode,
        RoundingMode capacityRoundingMode) {

    public enum PaymentFrequency {
        MONTHLY
    }

    public GoalCalculationPolicy {
        if (paymentFrequency == null) {
            throw new IllegalArgumentException("paymentFrequency is required");
        }
        if (monetaryScale < 0 || ratioScale < 0) {
            throw new IllegalArgumentException("scales must not be negative");
        }
        if (roundingMode == null || capacityRoundingMode == null) {
            throw new IllegalArgumentException("rounding modes are required");
        }
    }

    public static GoalCalculationPolicy defaults() {
        return new GoalCalculationPolicy(PaymentFrequency.MONTHLY, 2, 4,
                RoundingMode.HALF_UP, RoundingMode.CEILING);
    }
}
