package com.financialgps.application.roadmap.port.out;

import java.time.LocalDate;

/**
 * Business date provider for the roadmap when the client omits {@code asOf}.
 *
 * <p>Mirrors {@code GpsBusinessDate}: the application lane never reads a wall clock.
 */
public interface RoadmapBusinessDate {

    LocalDate today();
}
