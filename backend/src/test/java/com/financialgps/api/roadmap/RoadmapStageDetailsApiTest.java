package com.financialgps.api.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.financialgps.testsupport.AuthFlows;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T033 — the US2 stage-detail contract (RM-016, RM-018).
 *
 * <p>Proves the API exposes every stage detail the user needs to understand the route, keeps
 * {@code completionReached} independent of the positional {@code status}, always explains an
 * unavailable ETA, separates stage order from allocation, and never transports the 004 capacity
 * shortfall or any allocation/funding field.
 */
class RoadmapStageDetailsApiTest extends RoadmapApiSupport {

    @Test
    void everyStageExposesItsConditionsAndAnActionableSentence() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "15000000.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);

        assertThat(text(body.path("orderPolicy"), "scope")).isEqualTo("ROADMAP_STAGE_ORDER");
        assertThat(body.path("stages")).hasSize(2);

        JsonNode debt = stage(body, 0);
        assertThat(text(debt, "startCondition")).contains("No prerequisite");
        assertThat(text(debt, "completionCondition")).contains("COMPLETED");
        assertThat(text(debt, "nextAction")).isNotBlank().contains("planned payment");
        assertThat(debt.path("requiredMonthlyCapacity").isNull()).isTrue();
        assertThat(text(debt, "totalPlannedMonthlyPayment")).isEqualTo("7000000.00");

        JsonNode goal = stage(body, 1);
        assertThat(text(goal, "startCondition")).contains("Roadmap position only");
        assertThat(text(goal, "completionCondition")).isEqualTo("AMOUNT_REACHED");
        assertThat(text(goal, "nextAction")).isNotBlank()
                .contains("Stage 1 must complete")
                .contains("not mean it is unfunded");
        assertThat(text(goal, "requiredMonthlyCapacity")).isEqualTo("3750000.00");
        assertThat(goal.path("totalPlannedMonthlyPayment").isNull()).isTrue();

        assertThat(debt.path("nextAction").asText()).doesNotContain("shortfall");
        assertThat(goal.path("nextAction").asText()).doesNotContain("shortfall");
        assertEtasAreHonest(body);
        assertNoAllocationOrFundingKeys(body);
    }

    @Test
    void aReachedGoalKeepsCompletionBesideAnUpcomingPosition() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        // current == target: 003 stores the goal COMPLETED even though the debt stage is open.
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "60000000.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);
        JsonNode goal = stage(body, 1);

        assertThat(goal.path("completionReached").asBoolean()).isTrue();
        assertThat(text(goal, "status")).isEqualTo("UPCOMING");
        assertThat(goal.path("blockedBy").asInt()).isEqualTo(1);
        assertThat(text(goal, "progress")).isEqualTo("1.0000");
        assertThat(text(goal, "requiredMonthlyCapacity")).isEqualTo("0.00");
        assertThat(text(goal, "projectedCompletionDate")).isEqualTo(AS_OF);
        assertThat(text(goal, "nextAction"))
                .contains("already reached its target")
                .contains("not a funding statement");
        assertThat(body.path("explanations").toString())
                .contains("has already reached its own completion condition");
        assertNoAllocationOrFundingKeys(body);
    }

    @Test
    void anUnavailableEtaAlwaysCarriesItsReasonCode() throws Exception {
        String session = newOwner();
        putProfile(session, "VND"); // no income ⇒ Available Capacity 0.00
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", null); // no rate ⇒ blocked
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "0.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);
        JsonNode debt = stage(body, 0);
        JsonNode goal = stage(body, 1);

        assertThat(debt.path("projectedCompletionDate").isNull()).isTrue();
        assertThat(text(debt, "etaReasonCode")).isEqualTo("PORTFOLIO_CONTAINS_BLOCKED_DEBTS");
        assertThat(text(debt, "etaBasis")).isEqualTo("PORTFOLIO_PROJECTION");
        assertThat(text(debt, "etaAssumption")).isEqualTo("PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA");
        assertThat(debt.path("blockers")).hasSize(1);
        assertThat(text(debt.path("blockers").get(0), "code")).isEqualTo("INTEREST_RATE_MISSING");
        assertThat(text(debt, "nextAction")).contains("Resolve the blocked debts first");

        assertThat(goal.path("projectedCompletionDate").isNull()).isTrue();
        assertThat(text(goal, "etaReasonCode")).isEqualTo("NO_AVAILABLE_CAPACITY");
        assertThat(text(goal, "status")).isEqualTo("UPCOMING");
        assertThat(goal.path("blockedBy").asInt()).isEqualTo(1);
        assertThat(text(goal.path("blockers").get(0), "code"))
                .isEqualTo("NO_AVAILABLE_CAPACITY");

        assertEtasAreHonest(body);
        assertNoAllocationOrFundingKeys(body);
    }

    @Test
    void aMissingProfileIsReportedWithoutInventingACapacity() throws Exception {
        String session = newOwner();
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "0.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);
        JsonNode goal = stage(body, 0);

        assertThat(text(body, "currency")).isEqualTo("VND");
        assertThat(keysOf(body)).doesNotContain("availableCapacity");
        assertThat(goal.path("projectedCompletionDate").isNull()).isTrue();
        assertThat(text(goal, "etaReasonCode")).isEqualTo("MISSING_FINANCIAL_PROFILE");
        assertThat(text(goal.path("blockers").get(0), "code"))
                .isEqualTo("MISSING_FINANCIAL_PROFILE");
        assertThat(text(goal, "nextAction")).contains("Complete the Financial Profile");
        assertThat(text(goal, "requiredMonthlyCapacity")).isEqualTo("5000000.00");
        assertThat(body.path("explanations").toString())
                .contains("MISSING_FINANCIAL_PROFILE");
        assertNoAllocationOrFundingKeys(body);
    }

    @Test
    void explanationsSeparateStageOrderFromAllocationAndNameTheBlockingStage() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "0.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);
        String explanations = body.path("explanations").toString();

        assertThat(explanations)
                .contains("not an allocation order")
                .contains("Stage 2 is not current because stage 1 is not complete yet")
                .contains("its position does not mean it is unfunded")
                .contains("002 plan-based projection")
                .contains("single-destination hypothesis");
        assertThat(text(body.path("orderPolicy"), "scope")).isEqualTo("ROADMAP_STAGE_ORDER");
        assertThat(text(body.path("orderPolicy"), "description"))
                .contains("not the allocation order");
    }

    @Test
    void theCapacityShortfallAmountNeverLeaks() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "1000000.00");
        // Required 5,000,000/month vs 1,000,000 capacity → the 004 shortfall is 4,000,000.
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "0.00",
                TARGET, "1");

        String roadmapText = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", AS_OF)
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = JSON.readTree(roadmapText);
        JsonNode goal = stage(body, 0);

        assertThat(text(goal, "requiredMonthlyCapacity")).isEqualTo("5000000.00");
        assertThat(roadmapText).doesNotContain("4000000");
        assertThat(goal.path("nextAction").asText().toLowerCase()).doesNotContain("shortfall");
        assertThat(body.path("explanations").toString().toLowerCase()).doesNotContain("shortfall");
        assertNoAllocationOrFundingKeys(body);
    }

    @Test
    void debtEtaIsPlanBasedAndReservedRouteLabelsNeverAppear() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "0.00",
                TARGET, "1");

        String roadmapText = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", AS_OF)
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = JSON.readTree(roadmapText);
        JsonNode debt = stage(body, 0);

        assertThat(text(debt, "etaBasis")).isEqualTo("PORTFOLIO_PROJECTION");
        assertThat(text(debt, "etaAssumption"))
                .isEqualTo("PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA");
        assertThat(body.path("explanations").toString())
                .contains("002 plan-based projection")
                .contains("including each planned Debt Extra");
        assertThat(roadmapText)
                .doesNotContain("ROUTE_PROJECTION")
                .doesNotContain("ROUTE_ALLOCATION")
                .doesNotContain("FundedExtra")
                .doesNotContain("EffectivePayment")
                .doesNotContain("allocationStatus");
    }

    @Test
    void stageDetailKeysContainNoAllocationFundingOrShortfallField() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "0.00",
                TARGET, "1");

        JsonNode body = roadmapJson(session, AS_OF);
        for (JsonNode stage : body.path("stages")) {
            assertThat(keysOf(stage))
                    .doesNotContain("allocationStatus", "fundedAmount", "shortfallAmount",
                            "effectivePayment", "fundedExtra");
        }
        assertNoAllocationOrFundingKeys(body);
    }
}
