/**
 * Financial Roadmap application slice (Feature 005): models, ports and the read-only use case.
 *
 * <p>Direction of dependencies: {@code api → application → domain}. This package never imports
 * {@code infrastructure}, {@code api} or {@code platform} (enforced by
 * {@code com.financialgps.architecture.ArchitectureGuardTest}), and it holds no financial rule:
 * the roadmap arithmetic lives in {@code com.financialgps.domain.roadmap} and in the 002/003/004
 * domain services it reuses.
 */
package com.financialgps.application.roadmap;
