package com.financialgps.api.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T007: goal HTTP contract — status codes, validation matrix (SC1.2 → 400
 * VALIDATION_FAILED), derived remaining/progress, capacity coverage, lifecycle
 * transitions and the post-archive 404s (SC1.4, SC1.5).
 */
class GoalControllerTest extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    static String goalBody(String name, String type, String target, String current,
                           String date, String priority) {
        return "{\"name\":\"" + name + "\",\"goalType\":\"" + type + "\","
                + "\"targetAmount\":\"" + target + "\",\"currentAmount\":\"" + current + "\","
                + "\"targetDate\":" + (date == null ? "null" : ("\"" + date + "\"")) + ","
                + "\"priority\":" + priority + "}";
    }

    static String idOf(String body) throws Exception {
        return JSON.readTree(body).path("id").asText();
    }

    private String postGoal(String session, String target, String current, String date)
            throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Emergency Fund", "EMERGENCY_FUND", target, current,
                                date, "1")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void unauthenticatedGoalReadIs401() throws Exception {
        mockMvc.perform(get("/api/v1/goals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void createReturnsDerivedRemainingAndProgress() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Emergency Fund", "EMERGENCY_FUND", "120000000.00",
                                "30000000.00", "2027-12-31", "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("VND"))
                .andExpect(jsonPath("$.remaining").value("90000000.00"))
                .andExpect(jsonPath("$.progress").value("0.2500"))
                .andExpect(jsonPath("$.derived.remaining").value("calculated"))
                .andExpect(jsonPath("$.derived.currentAmount").value("actual"));
    }

    @Test
    void negativeTargetIs400() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("X", "SAVINGS", "-1.00", "0.00", null, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void negativeCurrentIs400() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("X", "SAVINGS", "100.00", "-5.00", null, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void malformedDecimalIs400() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("X", "SAVINGS", "100.123", "0.00", null, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void unknownGoalTypeIs400() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("X", "NOPE", "100.00", "0.00", null, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void zeroTargetIsCompletedAtCreation() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String body = postGoal(session, "0.00", "0.00", null);
        mockMvc.perform(get("/api/v1/goals/" + idOf(body)).cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value("1.0000"));
    }

    @Test
    void updateToTargetCompletesAndArchiveIsTerminal() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String id = idOf(postGoal(session, "120000000.00", "30000000.00", "2027-12-31"));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/goals/" + id))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Emergency Fund", "EMERGENCY_FUND", "120000000.00",
                                "120000000.00", "2027-12-31", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.remaining").value("0.00"));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/goals/" + id))
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/goals/" + id).cookie(AuthFlows.session(session)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/goals/" + id))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Emergency Fund", "EMERGENCY_FUND", "120000000.00",
                                "120000000.00", "2027-12-31", "1")))
                .andExpect(status().isNotFound());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/goals/" + id))
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusFilterNarrowsListAndRejectsUnknown() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        postGoal(session, "120000000.00", "30000000.00", "2027-12-31");
        postGoal(session, "100.00", "100.00", null);

        mockMvc.perform(get("/api/v1/goals?status=ACTIVE").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/goals?status=COMPLETED").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/goals?status=ARCHIVED").cookie(AuthFlows.session(session)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void capacityComparesAgainstAvailableCapacity() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        // Available Capacity 30M: profile + one 30M salary line.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"VND\",\"savingsAmount\":\"0.00\","
                                + "\"emergencyFundAmount\":\"0.00\",\"dependentsCount\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"30000000.00\",\"source\":\"salary\"}"))
                .andExpect(status().isCreated());

        // 90M remainder over 12 periods = 7.5M/month < 30M available.
        String inReach = LocalDate.now().plusMonths(12).toString();
        String id = idOf(postGoal(session, "120000000.00", "30000000.00", inReach));
        mockMvc.perform(get("/api/v1/goals/" + id + "/capacity")
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthsRemaining").value(12))
                .andExpect(jsonPath("$.requiredMonthlyCapacity").value("7500000.00"))
                .andExpect(jsonPath("$.availableCapacity").value("30000000.00"))
                .andExpect(jsonPath("$.capacityCoverage").value("MEETS_REQUIRED"))
                .andExpect(jsonPath("$.monthlyShortfall").value("0.00"));

        // Expired target: full remainder due now, shortfall against 30M capacity.
        String past = LocalDate.now().minusDays(1).toString();
        String expiredId = idOf(postGoal(session, "120000000.00", "30000000.00", past));
        mockMvc.perform(get("/api/v1/goals/" + expiredId + "/capacity")
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthsRemaining").value(0))
                .andExpect(jsonPath("$.requiredMonthlyCapacity").value("90000000.00"))
                .andExpect(jsonPath("$.capacityCoverage").value("SHORTFALL"))
                .andExpect(jsonPath("$.monthlyShortfall").value("60000000.00"))
                .andExpect(jsonPath("$.dateFeasibility").value("EXPIRED_TARGET_DATE"));

        // Undated goal: no schedule, NOT_APPLICABLE.
        String undatedId = idOf(postGoal(session, "90000000.00", "0.00", null));
        mockMvc.perform(get("/api/v1/goals/" + undatedId + "/capacity")
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredMonthlyCapacity").doesNotExist())
                .andExpect(jsonPath("$.capacityCoverage").value("NOT_APPLICABLE"))
                .andExpect(jsonPath("$.dateFeasibility").value("UNDATED"));
    }
}
