package com.financialgps.api.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Production ownership proof (P1 review finding): the fixture matrix proves the ownership
 * pattern, but only this test proves the real financial endpoints (profile, incomes, expenses)
 * actually enforce it. Two accounts are seeded; every cross-owner update/delete must answer 404
 * (indistinguishable from missing) and leave the victim's data untouched.
 */
class ProductionOwnershipIsolationTest extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    private void putProfile(String session) throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"VND\",\"savingsAmount\":\"0.00\","
                                + "\"emergencyFundAmount\":\"0.00\",\"dependentsCount\":0}"))
                .andExpect(status().isOk());
    }

    private String postIncome(String session) throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"50.00\",\"source\":\"salary\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String postExpense(String session) throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/expenses"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"5.00\",\"category\":\"rent\",\"expenseType\":\"FIXED\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static String idOf(String body) throws Exception {
        return JSON.readTree(body).path("id").asText();
    }

    private static Map<String, Object> problemWithoutInstance(String body) throws Exception {
        Map<String, Object> problem = JSON.readValue(body, new TypeReference<Map<String, Object>>() {
        });
        problem.remove("instance");
        return problem;
    }

    @Test
    void crossOwnerCannotMutateProductionLines() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        putProfile(sessionA);
        putProfile(sessionB);

        String incomeOfA = idOf(postIncome(sessionA));
        String expenseOfA = idOf(postExpense(sessionA));

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomes.length()").value(0))
                .andExpect(jsonPath("$.totalIncome.amount").value("0.00"));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/incomes/" + incomeOfA))
                        .cookie(AuthFlows.session(sessionB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"99.00\",\"source\":\"hijack\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/incomes/" + incomeOfA))
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/expenses/" + expenseOfA))
                        .cookie(AuthFlows.session(sessionB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"99.00\",\"category\":\"hijack\",\"expenseType\":\"FIXED\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/expenses/" + expenseOfA))
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(sessionA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.amount").value("50.00"))
                .andExpect(jsonPath("$.totalExpenses.amount").value("5.00"));
    }

    @Test
    void crossOwnerLineIdsAreIndistinguishableFromMissing() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        putProfile(sessionA);
        putProfile(sessionB);
        String incomeOfA = idOf(postIncome(sessionA));

        String crossOwnerBody = mockMvc.perform(AuthFlows.withCsrf(mockMvc,
                                put("/api/v1/incomes/" + incomeOfA))
                        .cookie(AuthFlows.session(sessionB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"99.00\",\"source\":\"hijack\"}"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        String missingBody = mockMvc.perform(AuthFlows.withCsrf(mockMvc,
                                put("/api/v1/incomes/" + UUID.randomUUID()))
                        .cookie(AuthFlows.session(sessionB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"99.00\",\"source\":\"hijack\"}"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(problemWithoutInstance(crossOwnerBody))
                .isEqualTo(problemWithoutInstance(missingBody));
    }
}
