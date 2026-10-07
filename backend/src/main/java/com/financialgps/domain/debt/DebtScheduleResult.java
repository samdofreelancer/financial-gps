package com.financialgps.domain.debt;

import java.util.List;
import java.util.Objects;

/**
 * Full amortization schedule paired with the same projection the debt card shows (spec §5.2).
 * BLOCKED and COMPLETED outcomes carry an empty row list — a blocked debt has no trustworthy
 * calendar, and the projection still carries its machine-readable reason (Constitution I + III).
 */
public record DebtScheduleResult(DebtProjectionResult projection, List<DebtScheduleEntry> rows) {

    public DebtScheduleResult {
        Objects.requireNonNull(projection, "projection");
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    }
}
