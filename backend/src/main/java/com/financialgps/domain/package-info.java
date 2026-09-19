/**
 * Pure Financial Domain Engine (bounded context: Financial Management / Financial GPS).
 *
 * <p>PURITY CONTRACT (specs/007-authentication/plan.md §Boundary, enforced by
 * {@code com.financialgps.platform.security.DomainBoundaryGuardTest}): no type in this package or
 * any subpackage may reference {@code org.springframework.*}, {@code jakarta.*},
 * {@code java.sql.*}, the platform/application/infrastructure packages, {@code java.time.Clock},
 * or {@code java.util.Random}. The engine never receives identity: no userId, no session, no
 * token, no HTTP request, no security context. Its only inputs are financial inputs, assumptions,
 * {@code asOfDate}, and financial policy.
 */
package com.financialgps.domain;
