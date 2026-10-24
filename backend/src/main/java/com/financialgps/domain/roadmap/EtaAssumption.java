package com.financialgps.domain.roadmap;

/**
 * What a projected completion date <b>assumes</b> — the label that keeps the ETA honest
 * (spec 005 §7.6, decision D-4).
 *
 * <ul>
 *   <li>{@link #PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA} — the stored 002 {@code plannedPayment}
 *       (minimum plus the <em>requested</em> Debt Extra) is paid in full. It is a plan-based
 *       assumption, not proof that the allocation funded the extra (RM-017).</li>
 *   <li>{@link #WHOLE_AVAILABLE_CAPACITY} — the whole Available Capacity is directed at this stage,
 *       ignoring competition between stages.</li>
 *   <li>{@link #ROUTE_ALLOCATION} — reserved for Feature 008's route-aware projection; never
 *       emitted by v1.</li>
 * </ul>
 */
public enum EtaAssumption {
    PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA,
    WHOLE_AVAILABLE_CAPACITY,
    /** Reserved for Feature 008's route-aware projection; never emitted by v1. */
    ROUTE_ALLOCATION
}
