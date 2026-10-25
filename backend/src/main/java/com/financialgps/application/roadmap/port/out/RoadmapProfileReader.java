package com.financialgps.application.roadmap.port.out;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.RoadmapInput;

import java.time.LocalDate;

/**
 * Port for reading the owner's route currency and Available Capacity.
 *
 * <p>Available Capacity is the canonical 004 quantity ({@code CurrentPositionCalculator}); the
 * currency is the stored 001 currency with the single 001 default. This port introduces neither a
 * second cash-flow formula nor a second default currency.
 */
public interface RoadmapProfileReader {

    RoadmapInput.ProfileSnapshot profile(OwnerId owner, LocalDate asOfDate);
}
