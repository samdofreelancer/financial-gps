package com.financialgps.domain.roadmap;

/**
 * Route-level status of the whole roadmap (spec 005 §7.4, decision D-6).
 *
 * <ul>
 *   <li>{@code IN_PROGRESS} — exactly one stage is {@link StageStatus#CURRENT};</li>
 *   <li>{@code COMPLETED} — at least one stage and every stage reached its completion condition;</li>
 *   <li>{@code NO_DESTINATION} — no stage at all (no ACTIVE debt and no stage-producing goal).</li>
 * </ul>
 */
public enum RouteStatus {
    IN_PROGRESS,
    COMPLETED,
    NO_DESTINATION
}
