/**
 * Output ports of the roadmap slice — the facts the roadmap reads, never a formula.
 *
 * <p>Every port is owner-scoped by construction and is implemented by an adapter in
 * {@code com.financialgps.infrastructure} (enforced by
 * {@code com.financialgps.architecture.ArchitectureGuardTest}):
 *
 * <ul>
 *   <li>{@code RoadmapGoalReader} — 003 goals, already ordered by {@code Goal.ORDERING},</li>
 *   <li>{@code RoadmapDebtReader} — the 002 plan-based portfolio projection,</li>
 *   <li>{@code RoadmapProfileReader} — the owner's currency and Available Capacity,</li>
 *   <li>{@code RoadmapBusinessDate} — the business date when the client omits {@code asOf}.</li>
 * </ul>
 */
package com.financialgps.application.roadmap.port.out;
