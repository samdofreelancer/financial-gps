package com.financialgps.application.roadmap.model;

import com.financialgps.domain.roadmap.EtaAssumption;
import com.financialgps.domain.roadmap.EtaBasis;
import com.financialgps.domain.roadmap.OrderPolicy;
import com.financialgps.domain.roadmap.ProvenanceEntry;
import com.financialgps.domain.roadmap.Roadmap;
import com.financialgps.domain.roadmap.RoadmapStage;
import com.financialgps.domain.roadmap.RouteStatus;
import com.financialgps.domain.roadmap.StageKind;
import com.financialgps.domain.roadmap.StageStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T046 (RM-018): the transport forwards a stage's ETA labels unchanged.
 *
 * <p>v1 never produces {@code ROUTE_PROJECTION}/{@code ROUTE_ALLOCATION} — those belong to Feature
 * 008 — but the read model must not rewrite a supplied 008 label either. This is the 005 → 008
 * seam: 005 renders whatever basis and assumption the stage carries, so a later feature can reuse
 * the route without a second mapping layer. No allocation loop or second ETA algorithm exists here.
 */
class RoadmapModelsTest {

    @Test
    void reservedRouteLabelsAreForwardedUnchanged() {
        RoadmapStage stage = new RoadmapStage(
                1,
                StageKind.GOAL,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "House fund",
                "Roadmap position only",
                "AMOUNT_REACHED",
                false,
                null,
                null,
                null,
                LocalDate.of(2027, 10, 24),
                EtaBasis.ROUTE_PROJECTION,
                EtaAssumption.ROUTE_ALLOCATION,
                null,
                StageStatus.CURRENT,
                null,
                List.of(),
                "Stay on the current plan.");

        Roadmap roadmap = new Roadmap(
                LocalDate.of(2026, 10, 24),
                "VND",
                RouteStatus.IN_PROGRESS,
                1,
                List.of(stage),
                OrderPolicy.defaults(),
                List.of("Stage order is not an allocation order."),
                List.of(ProvenanceEntry.calculated("routeStatus", "Derived from the stages")));

        RoadmapModels.RoadmapStageView view =
                RoadmapModels.RoadmapView.from(roadmap).stages().get(0);

        assertThat(view.etaBasis()).isEqualTo("ROUTE_PROJECTION");
        assertThat(view.etaAssumption()).isEqualTo("ROUTE_ALLOCATION");
        assertThat(view.projectedCompletionDate()).isEqualTo("2027-10-24");
        assertThat(view.etaReasonCode()).isNull();
    }
}
