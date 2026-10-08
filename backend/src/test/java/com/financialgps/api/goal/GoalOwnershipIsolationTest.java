package com.financialgps.api.goal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T008 (SC2.5): every goal endpoint is owner-scoped — User B sees User A's goal
 * as 404 RESOURCE_NOT_FOUND (indistinguishable from a missing id) on GET, PUT,
 * DELETE and capacity, B's list excludes A's goals, and A's data is untouched.
 */
class GoalOwnershipIsolationTest extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    private String postGoal(String session) throws Exception {
        String body = mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GoalControllerTest.goalBody("Emergency Fund", "EMERGENCY_FUND",
                                "120000000.00", "30000000.00", "2027-12-31", "1")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return GoalControllerTest.idOf(body);
    }

    private static Map<String, Object> problemWithoutInstance(String body) throws Exception {
        Map<String, Object> problem = JSON.readValue(body, new TypeReference<>() {
        });
        problem.remove("instance");
        return problem;
    }

    @Test
    void crossOwnerReadsAre404AndIndistinguishableFromMissing() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String idOfA = postGoal(sessionA);

        String crossOwnerBody = mockMvc.perform(get("/api/v1/goals/" + idOfA)
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andReturn().getResponse().getContentAsString();

        String missingBody = mockMvc.perform(get("/api/v1/goals/" + UUID.randomUUID())
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(problemWithoutInstance(crossOwnerBody))
                .isEqualTo(problemWithoutInstance(missingBody));

        mockMvc.perform(get("/api/v1/goals/" + idOfA + "/capacity")
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void crossOwnerMutationsAre404AndLeaveTheGoalUntouched() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String idOfA = postGoal(sessionA);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/goals/" + idOfA))
                        .cookie(AuthFlows.session(sessionB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GoalControllerTest.goalBody("Hijacked", "OTHER", "1.00", "1.00",
                                null, "9")))
                .andExpect(status().isNotFound());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/goals/" + idOfA))
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/goals/" + idOfA).cookie(AuthFlows.session(sessionA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Emergency Fund"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listIsOwnerScoped() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        postGoal(sessionA);
        postGoal(sessionA);

        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(get("/api/v1/goals").cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
