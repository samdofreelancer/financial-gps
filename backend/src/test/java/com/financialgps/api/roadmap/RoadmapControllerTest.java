package com.financialgps.api.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T013 — the roadmap HTTP contract (SC-004, RM-016, RM-018).
 *
 * <p>Pins the response shape, the kind-dependent capacity fields, the stage-order scope, the
 * optional {@code asOf}, the {@code NO_DESTINATION} payload, the {@code 400} for a malformed date
 * and the determinism of two identical reads. The financial rules themselves are covered by
 * {@code RoadmapGeneratorTest}; this class proves the transport exposes them unchanged.
 */
class RoadmapControllerTest extends RoadmapApiSupport {

    private static final java.util.Set<String> ROUTE_KEYS = java.util.Set.of(
            "asOf", "currency", "routeStatus", "currentStageOrder", "orderPolicy",
            "stages", "explanations", "provenance");

    private static final java.util.Set<String> STAGE_KEYS = java.util.Set.of(
            "order", "kind", "goalId", "objective", "startCondition", "completionCondition",
            "completionReached", "requiredMonthlyCapacity", "totalPlannedMonthlyPayment",
            "progress", "projectedCompletionDate", "etaBasis", "etaAssumption", "etaReasonCode",
            "status", "blockedBy", "blockers", "nextAction");

    @Test
    void roadmapDocumentHasExactlyTheContractedShape() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "15000000.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);

        assertThat(keysOf(body)).containsExactlyInAnyOrderElementsOf(ROUTE_KEYS);
        assertThat(text(body, "asOf")).isEqualTo(AS_OF);
        assertThat(text(body, "currency")).isEqualTo("VND");
        assertThat(text(body, "routeStatus")).isEqualTo("IN_PROGRESS");
        assertThat(body.path("currentStageOrder").asInt()).isEqualTo(1);
        assertThat(body.path("stages")).hasSize(1);
        assertThat(body.path("explanations")).isNotEmpty();
        assertThat(body.path("provenance")).isNotEmpty();

        JsonNode policy = body.path("orderPolicy");
        assertThat(keysOf(policy)).containsExactlyInAnyOrder(
                "policy", "scope", "description", "appliedOverrides");
        assertThat(text(policy, "policy")).isEqualTo("DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY");
        assertThat(text(policy, "scope")).isEqualTo("ROADMAP_STAGE_ORDER");
        assertThat(text(policy, "description")).contains("not the allocation order");
        assertThat(policy.path("appliedOverrides").size()).isZero();

        JsonNode stage = stage(body, 0);
        assertThat(keysOf(stage)).containsExactlyInAnyOrderElementsOf(STAGE_KEYS);
        assertThat(stage.path("order").asInt()).isEqualTo(1);
        assertThat(text(stage, "kind")).isEqualTo("GOAL");
        assertThat(stage.path("goalId").asText()).isNotBlank();
        assertThat(text(stage, "objective")).isEqualTo("Emergency fund");
        assertThat(text(stage, "completionCondition")).isEqualTo("AMOUNT_REACHED");
        assertThat(stage.path("completionReached").asBoolean()).isFalse();
        assertThat(text(stage, "requiredMonthlyCapacity")).isEqualTo("3750000.00");
        assertThat(stage.path("totalPlannedMonthlyPayment").isNull()).isTrue();
        assertThat(text(stage, "progress")).isEqualTo("0.2500");
        assertThat(text(stage, "projectedCompletionDate")).isEqualTo(AS_OF_DATE.plusMonths(2).toString());
        assertThat(text(stage, "etaBasis")).isEqualTo("SINGLE_DESTINATION_HYPOTHESIS");
        assertThat(text(stage, "etaAssumption")).isEqualTo("WHOLE_AVAILABLE_CAPACITY");
        assertThat(stage.path("etaReasonCode").isNull()).isTrue();
        assertThat(text(stage, "status")).isEqualTo("CURRENT");
        assertThat(stage.path("blockedBy").isNull()).isTrue();
        assertThat(stage.path("blockers").size()).isZero();
        assertThat(text(stage, "nextAction")).isNotBlank();
        assertThat(text(stage, "startCondition")).contains("Roadmap position only");

        assertEtasAreHonest(body);
        assertNoAllocationOrFundingKeys(body);
    }

    @Test
    void kindsCarryTheirOwnCapacityFieldOnly() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "15000000.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);
        assertThat(body.path("stages")).hasSize(2);

        JsonNode debt = stage(body, 0);
        assertThat(text(debt, "kind")).isEqualTo("DEBT_ELIMINATION");
        assertThat(debt.path("goalId").isNull()).isTrue();
        assertThat(debt.path("requiredMonthlyCapacity").isNull()).isTrue();
        assertThat(text(debt, "totalPlannedMonthlyPayment")).isEqualTo("7000000.00");
        assertThat(debt.path("progress").isNull()).isTrue();
        assertThat(text(debt, "etaBasis")).isEqualTo("PORTFOLIO_PROJECTION");
        assertThat(text(debt, "etaAssumption")).isEqualTo("PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA");
        assertThat(text(debt, "status")).isEqualTo("CURRENT");
        assertThat(debt.path("blockedBy").isNull()).isTrue();
        assertThat(text(debt, "projectedCompletionDate")).isNotBlank();

        JsonNode goal = stage(body, 1);
        assertThat(text(goal, "kind")).isEqualTo("GOAL");
        assertThat(goal.path("goalId").asText()).isNotBlank();
        assertThat(text(goal, "requiredMonthlyCapacity")).isEqualTo("3750000.00");
        assertThat(goal.path("totalPlannedMonthlyPayment").isNull()).isTrue();
        assertThat(text(goal, "progress")).isEqualTo("0.2500");
        assertThat(text(goal, "etaBasis")).isEqualTo("SINGLE_DESTINATION_HYPOTHESIS");
        assertThat(text(goal, "etaAssumption")).isEqualTo("WHOLE_AVAILABLE_CAPACITY");
        assertThat(text(goal, "status")).isEqualTo("UPCOMING");
        assertThat(goal.path("blockedBy").asInt()).isEqualTo(1);
        // Capacity 40M − 5M minimum = 35M; 45M remaining → ceil(1.29) = 2 periods.
        assertThat(text(goal, "projectedCompletionDate")).isEqualTo(AS_OF_DATE.plusMonths(2).toString());

        for (JsonNode stage : body.path("stages")) {
            assertThat(keysOf(stage)).contains("completionReached", "etaAssumption");
            if ("DEBT_ELIMINATION".equals(text(stage, "kind"))) {
                assertThat(stage.path("requiredMonthlyCapacity").isNull()).isTrue();
            } else {
                assertThat(stage.path("totalPlannedMonthlyPayment").isNull()).isTrue();
            }
        }
        assertNoAllocationOrFundingKeys(body);
    }

    @Test
    void debtStageForwardsThe002PlanBasedProjectionUnchanged() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "4000000.00");
        // Planned 5M against a 3.5M minimum: the 1.5M requested extra is larger than the 0.5M
        // Available Capacity, so this is exactly the RM-017 case — the date must stay plan-based.
        postDebt(session, "100000000.00", "3500000.00", "5000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "0.00",
                TARGET, "1");

        String roadmapText = mockMvc.perform(get("/api/v1/roadmap")
                        .cookie(com.financialgps.testsupport.AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = JSON.readTree(roadmapText);
        JsonNode debtStage = stage(body, 0);
        JsonNode summary = debtSummary(session);

        assertThat(text(debtStage, "etaBasis")).isEqualTo("PORTFOLIO_PROJECTION");
        assertThat(text(debtStage, "etaAssumption"))
                .isEqualTo("PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA");
        assertThat(text(debtStage, "projectedCompletionDate"))
                .isEqualTo(summary.path("portfolioProjection").path("projectedDebtFreeDate").asText());
        assertThat(body.path("explanations").toString())
                .contains("including each planned Debt Extra")
                .contains("not a route-funded date");

        assertThat(roadmapText).doesNotContain("ROUTE_PROJECTION").doesNotContain("ROUTE_ALLOCATION");
        assertThat(roadmapText).doesNotContain("1500000");
        assertThat(roadmapText).doesNotContain("FundedExtra").doesNotContain("EffectivePayment");
        assertNoAllocationOrFundingKeys(body);
        assertEtasAreHonest(body);
    }

    @Test
    void noDestinationIsAnEmptyExplainedRoute() throws Exception {
        String session = newOwner();
        JsonNode body = roadmapJson(session, AS_OF);

        assertThat(text(body, "routeStatus")).isEqualTo("NO_DESTINATION");
        assertThat(body.path("stages").size()).isZero();
        assertThat(body.path("currentStageOrder").isNull()).isTrue();
        assertThat(body.path("currency").asText()).isEqualTo("VND");
        assertThat(body.path("explanations")).hasSize(1);
        assertThat(body.path("explanations").get(0).asText()).contains("No destination is available");
        // No stage exists, so provenance explains only the route-level status.
        assertThat(body.path("provenance")).hasSize(1);
        assertThat(text(body.path("provenance").get(0), "field")).isEqualTo("routeStatus");
        assertThat(text(body.path("orderPolicy"), "scope")).isEqualTo("ROADMAP_STAGE_ORDER");

        // A DEBT_FREEDOM goal is a fact, not a stage: it still yields NO_DESTINATION (RM-011).
        String debtFreedomOwner = newOwner();
        postGoal(debtFreedomOwner, "Debt free", "DEBT_FREEDOM", null, null, null, "1");
        JsonNode debtFreedomRoute = roadmapJson(debtFreedomOwner, AS_OF);
        assertThat(text(debtFreedomRoute, "routeStatus")).isEqualTo("NO_DESTINATION");
        assertThat(debtFreedomRoute.path("stages").size()).isZero();
    }

    @Test
    void omittedAsOfIsResolvedByTheBusinessDateAndEchoed() throws Exception {
        String session = newOwner();
        LocalDate before = LocalDate.now(ZoneOffset.UTC);
        JsonNode body = roadmapJson(session, null);
        LocalDate after = LocalDate.now(ZoneOffset.UTC);

        LocalDate evaluated = LocalDate.parse(text(body, "asOf"));
        assertThat(evaluated).isBetween(before, after);
    }

    @Test
    void malformedAsOfIs400ValidationFailedWithAnAsOfViolation() throws Exception {
        String session = newOwner();

        String notADate = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", "not-a-date")
                        .cookie(com.financialgps.testsupport.AuthFlows.session(session)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(JSON.readTree(notADate).path("violations")).hasSize(1);
        assertThat(JSON.readTree(notADate).path("violations").get(0).asText())
                .isEqualTo("asOf: must be a valid ISO date");

        String impossibleDate = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", "2026-13-99")
                        .cookie(com.financialgps.testsupport.AuthFlows.session(session)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(JSON.readTree(impossibleDate).path("violations").get(0).asText())
                .isEqualTo("asOf: must be a valid ISO date");
    }

    @Test
    void twoIdenticalReadsReturnAByteIdenticalBody() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "15000000.00",
                TARGET, "1");

        MvcResult first = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", AS_OF)
                        .cookie(com.financialgps.testsupport.AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", AS_OF)
                        .cookie(com.financialgps.testsupport.AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(Arrays.equals(second.getResponse().getContentAsByteArray(),
                first.getResponse().getContentAsByteArray())).isTrue();
    }
}
