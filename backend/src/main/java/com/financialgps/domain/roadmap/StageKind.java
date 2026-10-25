package com.financialgps.domain.roadmap;

/**
 * What a stage is on the route.
 *
 * <p>{@code DEBT_ELIMINATION} exists while the 002 portfolio still carries ACTIVE debt (projection
 * status {@code AVAILABLE} or {@code BLOCKED}); {@code GOAL} mirrors one non-archived,
 * non-{@code DEBT_FREEDOM} 003 goal. A {@code DEBT_FREEDOM} goal never creates its own stage: its
 * completion condition <em>is</em> the debt stage's condition (spec 005 §7.1).
 */
public enum StageKind {
    DEBT_ELIMINATION,
    GOAL
}
