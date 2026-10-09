package com.financialgps.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Single source of truth for effective-dating: a line contributes only when it is
 * active and its effectiveFrom is on or before asOf.
 */
public final class EffectiveDating {

    private EffectiveDating() {
    }

    public static boolean isEffectiveOn(boolean active, LocalDate effectiveFrom, LocalDate asOf) {
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(asOf, "asOf");
        return active && !effectiveFrom.isAfter(asOf);
    }
}
