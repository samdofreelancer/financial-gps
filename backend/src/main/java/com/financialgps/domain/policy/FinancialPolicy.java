package com.financialgps.domain.policy;

import java.math.RoundingMode;

/**
 * Immutable financial policy (subset needed for 001). Rounding defaults per
 * calculation-rules §1: HALF_UP display, CEILING for counts that must not
 * overstate a full contribution.
 */
public record FinancialPolicy(RoundingMode displayRounding, RoundingMode countRounding) {

    public static FinancialPolicy defaults() {
        return new FinancialPolicy(RoundingMode.HALF_UP, RoundingMode.CEILING);
    }
}
