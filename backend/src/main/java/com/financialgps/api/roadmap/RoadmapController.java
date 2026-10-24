package com.financialgps.api.roadmap;

import com.financialgps.application.roadmap.model.RoadmapModels;
import com.financialgps.application.roadmap.port.in.GetRoadmap;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.platform.security.CurrentOwnerProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * HTTP adapter for the Financial Roadmap.
 * {@code GET /api/v1/roadmap?asOf=...} — read-only, owner taken from the session.
 *
 * <p>The controller owns no financial rule and no clock: an omitted {@code asOf} is resolved by the
 * use case through the business-date port, and every derived value comes from the application view.
 */
@RestController
@RequestMapping("/api/v1/roadmap")
public class RoadmapController {

    private final GetRoadmap getRoadmap;
    private final CurrentOwnerProvider owners;

    public RoadmapController(GetRoadmap getRoadmap, CurrentOwnerProvider owners) {
        this.getRoadmap = getRoadmap;
        this.owners = owners;
    }

    @GetMapping
    public RoadmapModels.RoadmapView roadmap(
            @RequestParam(required = false) LocalDate asOf) {
        OwnerId owner = owners.requireCurrentOwner();
        if (asOf != null) {
            return getRoadmap.roadmap(owner, asOf);
        }
        return getRoadmap.roadmap(owner);
    }
}
