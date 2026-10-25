package com.financialgps.domain.roadmap;

import java.util.Objects;

/**
 * A verifiable condition that prevents or delays a stage, in the shared {@code {code, explanation}}
 * shape (spec 005 §7.3).
 *
 * <p>The {@code code} is never invented: a goal stage uses a 004 {@code GpsBlocker.Code} value
 * ({@code MISSING_FINANCIAL_PROFILE}, {@code NO_AVAILABLE_CAPACITY},
 * {@code PAYOFF_HORIZON_EXCEEDS_MAXIMUM}, …), the debt stage uses a 002 blocked-debt reason code.
 * {@code UNDATED_GOAL} and {@code CAPACITY_SHORTFALL} are 004 next-action input tokens, not blocker
 * codes, and are never emitted here.
 */
public record Blocker(String code, String explanation) {

    public Blocker {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(explanation, "explanation");
    }
}
