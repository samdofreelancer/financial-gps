package com.financialgps.application.roadmap.model;

import com.financialgps.domain.roadmap.Blocker;
import com.financialgps.domain.roadmap.OrderPolicy;
import com.financialgps.domain.roadmap.ProvenanceEntry;
import com.financialgps.domain.roadmap.Roadmap;
import com.financialgps.domain.roadmap.RoadmapStage;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Roadmap REST DTOs (spec 005 §8, {@code contracts/rest-api.md}).
 *
 * <p>Transport rules are those of 001–004: amounts and ratios are decimal <b>strings</b>, dates are
 * ISO-8601, {@code currency} is echoed once at route level and a value that is not applicable or not
 * calculable is {@code null} <em>with</em> a reason code, a blocker or an explanation — never an
 * available {@code "0.00"}.
 *
 * <p>The model carries no {@code allocationStatus}, funded amount or shortfall amount: Feature 005
 * has no allocation data to report (spec §7.2, RM-016).
 */
public final class RoadmapModels {

    private RoadmapModels() {
    }

    public record RoadmapView(
            String asOf,
            String currency,
            String routeStatus,
            Integer currentStageOrder,
            OrderPolicyView orderPolicy,
            List<RoadmapStageView> stages,
            List<String> explanations,
            List<ProvenanceView> provenance) {

        public RoadmapView {
            Objects.requireNonNull(asOf, "asOf");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(routeStatus, "routeStatus");
            Objects.requireNonNull(orderPolicy, "orderPolicy");
            Objects.requireNonNull(stages, "stages");
            Objects.requireNonNull(explanations, "explanations");
            Objects.requireNonNull(provenance, "provenance");
        }

        public static RoadmapView from(Roadmap roadmap) {
            return new RoadmapView(
                    roadmap.asOf().toString(),
                    roadmap.currency(),
                    roadmap.routeStatus().name(),
                    roadmap.currentStageOrder(),
                    OrderPolicyView.from(roadmap.orderPolicy()),
                    roadmap.stages().stream().map(RoadmapStageView::from).collect(Collectors.toList()),
                    roadmap.explanations(),
                    roadmap.provenance().stream().map(ProvenanceView::from).collect(Collectors.toList()));
        }
    }

    public record RoadmapStageView(
            int order,
            String kind,
            String goalId,
            String objective,
            String startCondition,
            String completionCondition,
            boolean completionReached,
            String requiredMonthlyCapacity,
            String totalPlannedMonthlyPayment,
            String progress,
            String projectedCompletionDate,
            String etaBasis,
            String etaAssumption,
            String etaReasonCode,
            String status,
            Integer blockedBy,
            List<BlockerView> blockers,
            String nextAction) {

        public RoadmapStageView {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(startCondition, "startCondition");
            Objects.requireNonNull(completionCondition, "completionCondition");
            Objects.requireNonNull(etaBasis, "etaBasis");
            Objects.requireNonNull(etaAssumption, "etaAssumption");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(blockers, "blockers");
            Objects.requireNonNull(nextAction, "nextAction");
        }

        public static RoadmapStageView from(RoadmapStage stage) {
            return new RoadmapStageView(
                    stage.order(),
                    stage.kind().name(),
                    stage.goalId() == null ? null : stage.goalId().toString(),
                    stage.objective(),
                    stage.startCondition(),
                    stage.completionCondition(),
                    stage.completionReached(),
                    stage.requiredMonthlyCapacity() == null
                            ? null : stage.requiredMonthlyCapacity().asDecimalString(),
                    stage.totalPlannedMonthlyPayment() == null
                            ? null : stage.totalPlannedMonthlyPayment().asDecimalString(),
                    stage.progress() == null ? null : stage.progress().toPlainString(),
                    stage.projectedCompletionDate() == null
                            ? null : stage.projectedCompletionDate().toString(),
                    stage.etaBasis().name(),
                    stage.etaAssumption().name(),
                    stage.etaReasonCode(),
                    stage.status().name(),
                    stage.blockedBy(),
                    stage.blockers().stream().map(BlockerView::from).collect(Collectors.toList()),
                    stage.nextAction());
        }
    }

    public record OrderPolicyView(
            String policy,
            String scope,
            String description,
            List<String> appliedOverrides) {

        public OrderPolicyView {
            Objects.requireNonNull(policy, "policy");
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(appliedOverrides, "appliedOverrides");
        }

        public static OrderPolicyView from(OrderPolicy policy) {
            return new OrderPolicyView(policy.policy(), policy.scope(), policy.description(),
                    policy.appliedOverrides());
        }
    }

    public record BlockerView(String code, String explanation) {

        public static BlockerView from(Blocker blocker) {
            return new BlockerView(blocker.code(), blocker.explanation());
        }
    }

    public record ProvenanceView(String field, String kind, String detail) {

        public static ProvenanceView from(ProvenanceEntry entry) {
            return new ProvenanceView(entry.field(), entry.kind(), entry.detail());
        }
    }
}
