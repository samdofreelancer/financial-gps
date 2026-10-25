package com.financialgps.domain.roadmap;

/**
 * Where a stage's projected completion date comes from (spec 005 §7.6).
 *
 * <p>v1 emits only {@link #PORTFOLIO_PROJECTION} (the 002 plan-based quantity for the debt stage)
 * and {@link #SINGLE_DESTINATION_HYPOTHESIS} (the 004 {@code ceilPeriods} quantity for a goal
 * stage). {@link #ROUTE_PROJECTION} is <b>reserved for Feature 008</b>: when the route-aware
 * projection ships, 005 forwards that date and label unchanged and never recomputes it (RM-018).
 */
public enum EtaBasis {
    PORTFOLIO_PROJECTION,
    SINGLE_DESTINATION_HYPOTHESIS,
    /** Reserved for Feature 008's route-aware projection; never emitted by v1. */
    ROUTE_PROJECTION
}
