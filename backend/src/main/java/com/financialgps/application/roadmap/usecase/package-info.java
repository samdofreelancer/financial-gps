/**
 * Roadmap use case (application service): read the owner's facts, assemble the domain input, call
 * the pure generator and map the result to the transport view.
 *
 * <p>Assembly only — the dates come from the {@code RoadmapBusinessDate} port, and no financial
 * formula lives here (constitution §VIII, §XIV).
 */
package com.financialgps.application.roadmap.usecase;
