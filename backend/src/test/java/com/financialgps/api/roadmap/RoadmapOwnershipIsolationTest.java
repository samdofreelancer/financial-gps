package com.financialgps.api.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.financialgps.testsupport.AuthFlows;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T014 (FR-007, SC2.5, RM-014): the roadmap is owner-scoped through the session, never through a
 * request parameter.
 *
 * <p>No endpoint accepts an owner or a goal identifier, so the isolation claim is exactly that: the
 * route reported for a session contains only that owner's facts, a second owner's roadmap is
 * unaffected by the first owner's data, and the borrowed 003 goal endpoints still hide another
 * owner's goal behind {@code 404 RESOURCE_NOT_FOUND} (never {@code 403}). The final test pins the
 * read-only promise: reading the roadmap changes nothing.
 */
class RoadmapOwnershipIsolationTest extends RoadmapApiSupport {

    @Test
    void eachOwnerSeesOnlyTheirOwnFacts() throws Exception {
        String ownerA = newOwner();
        putProfile(ownerA, "VND");
        postIncome(ownerA, "40000000.00");
        postDebt(ownerA, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        String goalOfA = postGoal(ownerA, "Owner A emergency fund", "EMERGENCY_FUND",
                "60000000.00", "15000000.00", TARGET, "1");

        // A never sets a profile, an income or a goal: the route has nothing to show.
        String ownerB = newOwner();
        String emptyRouteText = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", AS_OF)
                        .cookie(AuthFlows.session(ownerB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode emptyRoute = JSON.readTree(emptyRouteText);
        assertThat(text(emptyRoute, "routeStatus")).isEqualTo("NO_DESTINATION");
        assertThat(emptyRoute.path("stages").size()).isZero();
        assertThat(emptyRouteText)
                .doesNotContain(goalOfA)
                .doesNotContain("Owner A emergency fund")
                .doesNotContain("40000000");

        // B's own route shows B's goal only.
        String goalOfB = postGoal(ownerB, "Owner B emergency fund", "EMERGENCY_FUND",
                "12000000.00", "0.00", TARGET, "1");
        JsonNode routeOfB = roadmapJson(ownerB, AS_OF);
        assertThat(routeOfB.path("stages")).hasSize(1);
        assertThat(text(stage(routeOfB, 0), "goalId")).isEqualTo(goalOfB);
        assertThat(routeOfB.toString()).doesNotContain(goalOfA);

        // A's route is untouched by B's goal and still owns exactly A's two stages.
        JsonNode routeOfA = roadmapJson(ownerA, AS_OF);
        assertThat(routeOfA.path("stages")).hasSize(2);
        assertThat(text(stage(routeOfA, 1), "goalId")).isEqualTo(goalOfA);
        assertThat(routeOfA.toString())
                .doesNotContain(goalOfB)
                .doesNotContain("Owner B emergency fund");
        assertNoAllocationOrFundingKeys(routeOfA);
    }

    @Test
    void anOwnerParameterCannotSelectAnotherOwnersRoute() throws Exception {
        String ownerA = newOwner();
        putProfile(ownerA, "VND");
        postGoal(ownerA, "Owner A emergency fund", "EMERGENCY_FUND", "60000000.00", "15000000.00",
                TARGET, "1");

        String ownerB = newOwner();
        String bodyText = mockMvc.perform(get("/api/v1/roadmap")
                        .param("asOf", AS_OF)
                        .param("ownerId", UUID.randomUUID().toString())
                        .cookie(AuthFlows.session(ownerB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode route = JSON.readTree(bodyText);
        assertThat(text(route, "routeStatus")).isEqualTo("NO_DESTINATION");
        assertThat(route.path("stages").size()).isZero();
    }

    @Test
    void crossOwnerGoalAccessIs404ResourceNotFoundAndLeaksNoGoalId() throws Exception {
        String ownerA = newOwner();
        putProfile(ownerA, "VND");
        String goalOfA = postGoal(ownerA, "Owner A emergency fund", "EMERGENCY_FUND",
                "60000000.00", "15000000.00", TARGET, "1");

        String ownerB = newOwner();
        mockMvc.perform(get("/api/v1/goals/" + goalOfA)
                        .cookie(AuthFlows.session(ownerB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/goals/" + goalOfA + "/capacity")
                        .cookie(AuthFlows.session(ownerB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        JsonNode routeOfB = roadmapJson(ownerB, AS_OF);
        assertThat(routeOfB.toString()).doesNotContain(goalOfA);
    }

    @Test
    void readingTheRoadmapMutatesNothing() throws Exception {
        String session = newOwner();
        putProfile(session, "VND");
        postIncome(session, "40000000.00");
        postDebt(session, "100000000.00", "5000000.00", "7000000.00", "0.120000");
        postGoal(session, "Emergency fund", "EMERGENCY_FUND", "60000000.00", "15000000.00",
                TARGET, "1");

        String goalsBefore = mockMvc.perform(get("/api/v1/goals")
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String summaryBefore = debtSummary(session).toString();

        roadmapJson(session, AS_OF);
        roadmapJson(session, AS_OF);

        String goalsAfter = mockMvc.perform(get("/api/v1/goals")
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JSON.readTree(goalsAfter)).isEqualTo(JSON.readTree(goalsBefore));
        assertThat(debtSummary(session).toString()).isEqualTo(summaryBefore);
    }
}
