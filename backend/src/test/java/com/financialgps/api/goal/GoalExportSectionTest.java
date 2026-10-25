package com.financialgps.api.goal;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec §7: the {@code goals} export section is registered in
 * {@code ExportOwnerDataUseCase} — a created goal appears as a row with its facts,
 * and account deletion cascades at the database level (FK {@code ON DELETE CASCADE}).
 */
class GoalExportSectionTest extends IntegrationTestBase {

    @Test
    void createdGoalAppearsInOwnerExport() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GoalControllerTest.goalBody("Emergency Fund", "EMERGENCY_FUND",
                                "120000000.00", "30000000.00", "2027-12-31", "1")))
                .andExpect(status().isCreated());

        mockMvc.perform(AuthFlows.export(session, AuthFlows.PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals").isArray())
                .andExpect(jsonPath("$.goals.length()").value(1))
                .andExpect(jsonPath("$.goals[0].name").value("Emergency Fund"))
                .andExpect(jsonPath("$.goals[0].goalType").value("EMERGENCY_FUND"))
                .andExpect(jsonPath("$.goals[0].targetAmount").value("120000000.00"))
                .andExpect(jsonPath("$.goals[0].currentAmount").value("30000000.00"))
                .andExpect(jsonPath("$.goals[0].targetDate").value("2027-12-31"))
                .andExpect(jsonPath("$.goals[0].status").value("ACTIVE"));
    }

    @Test
    void archivedGoalLeavesTheExport() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String body = mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GoalControllerTest.goalBody("Emergency Fund", "EMERGENCY_FUND",
                                "120000000.00", "30000000.00", "2027-12-31", "1")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = GoalControllerTest.idOf(body);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc,
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .delete("/api/v1/goals/" + id))
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isNoContent());

        mockMvc.perform(AuthFlows.export(session, AuthFlows.PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals.length()").value(0));
    }
}
