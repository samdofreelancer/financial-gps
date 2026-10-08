package com.financialgps.domain.goal;

/** Lifecycle states (spec §4.5). ARCHIVED is a soft-delete tombstone, terminal for user flows. */
public enum GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}
