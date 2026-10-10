package com.financialgps.domain.goal;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pure capacity projection for one goal (spec §4.3). */
public record GoalCapacity(
        Money remaining,
        Integer monthsRemaining,
        Money requiredMonthlyCapacity,
        Money availableCapacity,
        CapacityCoverage capacityCoverage,
        Money monthlyShortfall,
        DateFeasibility dateFeasibility,
        LocalDate asOf,
        String explanation) {

    public enum CapacityCoverage {
        MEETS_REQUIRED,
        SHORTFALL,
        NOT_APPLICABLE
    }

    public enum DateFeasibility {
        DATED,
        UNDATED,
        EXPIRED_TARGET_DATE,
        COMPLETED,
        /**
         * The goal completes on the Feature 002 debt-freedom condition rather than a target
         * amount/date (spec §4.5, decision D-6): remaining capacity is not applicable here — its
         * distance is the 002 portfolio outstanding, exposed by Feature 004.
         */
        DEBT_FREE
    }
}
