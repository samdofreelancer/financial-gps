/**
 * Pure Financial Roadmap slice (Feature 005).
 *
 * <p>The roadmap turns the owner's already-stored facts (001 profile, 002 debt portfolio, 003
 * goals) into one ordered route of <em>stages</em>. It is a derived read model: nothing here is
 * persisted, and reading never mutates a stored fact.
 *
 * <p><b>PURITY CONTRACT</b> (same rule as {@code com.financialgps.domain}, enforced by
 * {@code com.financialgps.platform.security.DomainBoundaryGuardTest}): no type in this package or
 * any subpackage may reference {@code org.springframework.*}, {@code jakarta.*},
 * {@code java.sql.*}, the platform/application/infrastructure packages,
 * {@code java.time.Clock} or {@code java.util.Random}. The {@code asOf} date is always passed in
 * explicitly — the domain never reads a clock.
 *
 * <p><b>No money formula, no allocation.</b> Every amount the roadmap reports is a fact or an
 * already-computed quantity borrowed from its owner feature:
 *
 * <ul>
 *   <li>available capacity — {@code domain.gps.CurrentPositionCalculator} (reached through the
 *       profile adapter),</li>
 *   <li>goal progress / required capacity — {@code domain.goal.GoalProgressCalculator} and
 *       {@code domain.goal.GoalCapacityCalculator},</li>
 *   <li>goal completion date hypothesis — {@code domain.gps.GpsEtaCalculator.ceilPeriods},</li>
 *   <li>debt-stage facts and the plan-based payoff date — the 002 portfolio projection.</li>
 * </ul>
 *
 * <p>The roadmap <b>orders stages for presentation</b>. That order is not the per-period allocation
 * order of {@code specs/financial-domain/calculation-rules.md} §8 (owned by Feature 008); stage
 * position never gates funding, a later stage may already hold money, and no allocation status,
 * funded amount or shortfall amount is emitted. See §8.1 of that oracle for the separation.
 */
package com.financialgps.domain.roadmap;
