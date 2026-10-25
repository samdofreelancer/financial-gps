package com.financialgps.domain.roadmap;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Generator-internal projection of one stage: every fact and figure, with no position on the route
 * yet.
 *
 * <p>It exists so {@link RoadmapGenerator} can evaluate every stage's <b>own</b> completion
 * condition first and only then assign the positional {@link StageStatus} — the order the spec
 * requires (compute completion, then position). It is package-private and not part of the wire
 * model.
 */
record RoadmapStageDraft(
        int order,
        StageKind kind,
        UUID goalId,
        String objective,
        String startCondition,
        String completionCondition,
        boolean completionReached,
        Money requiredMonthlyCapacity,
        Money totalPlannedMonthlyPayment,
        BigDecimal progress,
        RoadmapEtaHypothesis eta,
        List<Blocker> blockers,
        Signals signals) {

    RoadmapStageDraft {
        blockers = List.copyOf(blockers);
    }

    /**
     * State facts the next-action sentence depends on. None of them is a blocker code of its own
     * unless it is also present in {@link #blockers()}.
     */
    record Signals(
            boolean profileMissing,
            boolean capacityZero,
            boolean undated,
            boolean portfolioBlocked) {
    }
}
