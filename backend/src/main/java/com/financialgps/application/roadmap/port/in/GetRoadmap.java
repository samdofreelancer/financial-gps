package com.financialgps.application.roadmap.port.in;

import com.financialgps.application.roadmap.model.RoadmapModels;
import com.financialgps.domain.model.OwnerId;

import java.time.LocalDate;

/**
 * Input port for reading the owner's Financial Roadmap (Feature 005).
 *
 * <p>Read-only by design: the roadmap is a derived projection of facts the owner already stored
 * (001 profile, 002 debt, 003 goals), so the feature has no write operation at all.
 */
public interface GetRoadmap {

    /** The route evaluated as of an explicit business date (deterministic, echoed in the view). */
    RoadmapModels.RoadmapView roadmap(OwnerId owner, LocalDate asOfDate);

    /** The route evaluated as of today, supplied by the {@code RoadmapBusinessDate} port. */
    RoadmapModels.RoadmapView roadmap(OwnerId owner);
}
