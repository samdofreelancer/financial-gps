/**
 * Input ports of the roadmap slice.
 *
 * <p>{@code GetRoadmap} is the only entry point: the roadmap is a read-only projection (Feature 005
 * adds no write operation). The boundary is transaction-demarcated by
 * {@code infrastructure.configuration.RoadmapUseCaseConfiguration} through
 * {@code UseCaseTransactions.readOnly(...)} — a use case never carries a framework annotation.
 */
package com.financialgps.application.roadmap.port.in;
