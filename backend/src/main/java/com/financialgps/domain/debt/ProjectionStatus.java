package com.financialgps.domain.debt;

/** Projection outcome shared by single-debt and portfolio projections (spec §5.3–§5.4). */
public enum ProjectionStatus {
    AVAILABLE,
    BLOCKED,
    COMPLETED
}
