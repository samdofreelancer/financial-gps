package com.financialgps.domain.roadmap;

/**
 * A stage's <b>position on the roadmap</b> — never an allocation or funding status.
 *
 * <p>{@code UPCOMING} means "not current on the route yet". It does not mean the stage is
 * unfunded, and it never says money must not be directed at it: the per-period allocation order is
 * Feature 008's, and a later stage may already hold money while an earlier stage is incomplete
 * (spec 005 §7.2, §7.4, RM-016).
 *
 * <p>This enum does not redefine the 004 Financial Status vocabulary ({@code ON_TRACK},
 * {@code AT_RISK}, {@code OFF_TRACK}, {@code BLOCKED}, {@code COMPLETED}).
 */
public enum StageStatus {
    COMPLETED,
    CURRENT,
    UPCOMING
}
