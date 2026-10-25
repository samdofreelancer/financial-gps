package com.financialgps.domain.roadmap;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The ordered route of the owner (spec 005 §7). A pure read model: no persistence, no lifecycle,
 * no allocation rows.
 *
 * <p>Invariants: {@code stages} are contiguous from 1; {@code NO_DESTINATION} ⟺ no stages;
 * {@code COMPLETED} ⟺ stages exist and all are completed; {@code IN_PROGRESS} ⟺ exactly one stage
 * is {@code CURRENT}; {@code currentStageOrder} is {@code null} unless the route is in progress.
 */
public record Roadmap(
        LocalDate asOf,
        String currency,
        RouteStatus routeStatus,
        Integer currentStageOrder,
        List<RoadmapStage> stages,
        OrderPolicy orderPolicy,
        List<String> explanations,
        List<ProvenanceEntry> provenance) {

    public Roadmap {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(routeStatus, "routeStatus");
        Objects.requireNonNull(stages, "stages");
        Objects.requireNonNull(orderPolicy, "orderPolicy");
        Objects.requireNonNull(explanations, "explanations");
        Objects.requireNonNull(provenance, "provenance");
        stages = List.copyOf(stages);
        explanations = List.copyOf(explanations);
        provenance = List.copyOf(provenance);

        for (int index = 0; index < stages.size(); index++) {
            if (stages.get(index).order() != index + 1) {
                throw new IllegalArgumentException("stages must be ordered 1..n without gaps");
            }
        }
        long current = stages.stream().filter(s -> s.status() == StageStatus.CURRENT).count();
        long completed = stages.stream().filter(s -> s.status() == StageStatus.COMPLETED).count();
        switch (routeStatus) {
            case NO_DESTINATION -> {
                if (!stages.isEmpty()) {
                    throw new IllegalArgumentException("NO_DESTINATION means no stage");
                }
                if (currentStageOrder != null) {
                    throw new IllegalArgumentException("NO_DESTINATION has no current stage");
                }
            }
            case COMPLETED -> {
                if (stages.isEmpty()) {
                    throw new IllegalArgumentException("a COMPLETED route has stages");
                }
                if (completed != stages.size()) {
                    throw new IllegalArgumentException("a COMPLETED route has only completed stages");
                }
                if (currentStageOrder != null) {
                    throw new IllegalArgumentException("a COMPLETED route has no current stage");
                }
            }
            case IN_PROGRESS -> {
                if (current != 1) {
                    throw new IllegalArgumentException("an IN_PROGRESS route has exactly one current stage");
                }
                RoadmapStage currentStage = stages.stream()
                        .filter(s -> s.status() == StageStatus.CURRENT)
                        .findFirst()
                        .orElseThrow();
                if (currentStageOrder == null || currentStageOrder != currentStage.order()) {
                    throw new IllegalArgumentException("currentStageOrder points at the CURRENT stage");
                }
            }
        }
    }
}
