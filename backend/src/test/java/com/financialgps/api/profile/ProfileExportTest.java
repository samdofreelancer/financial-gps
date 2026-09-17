package com.financialgps.api.profile;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 001 (Slice 3 / A2 + Slice 6 / I1): the 007 export bundle must carry the Financial Profile rows —
 * deterministic, owner-scoped, and empty arrays (never omitted) before the first write.
 */
class ProfileExportTest extends IntegrationTestBase {

    private static final String PROFILE = "{\"currency\":\"VND\",\"savingsAmount\":\"10.00\","
            + "\"emergencyFundAmount\":\"5.00\",\"dependentsCount\":2}";

    @Test
    void exportContainsProfileIncomesAndExpensesForTheSignedInOwnerOnly() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isOk());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"74.00\",\"source\":\"salary\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/expenses"))
                        .cookie(AuthFlows.session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"30.00\",\"category\":\"rent\",\"expenseType\":\"FIXED\"}"))
                .andExpect(status().isCreated());

        String first = mockMvc.perform(get("/api/v1/account/export").cookie(AuthFlows.session(sessionA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.length()").value(1))
                .andExpect(jsonPath("$.profile[0].savingsAmount").value("10.00"))
                .andExpect(jsonPath("$.profile[0].currency").value("VND"))
                .andExpect(jsonPath("$.incomes.length()").value(1))
                .andExpect(jsonPath("$.incomes[0].amount").value("74.00"))
                .andExpect(jsonPath("$.expenses.length()").value(1))
                .andExpect(jsonPath("$.expenses[0].expenseType").value("FIXED"))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/v1/account/export").cookie(AuthFlows.session(sessionA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(second).as("deterministic bundle (SC-006)").isEqualTo(first);

        // Owner B has written nothing: arrays stay present and empty, and no A data leaks.
        String other = mockMvc.perform(get("/api/v1/account/export").cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.length()").value(0))
                .andExpect(jsonPath("$.incomes.length()").value(0))
                .andReturn().getResponse().getContentAsString();
        assertThat(other).doesNotContain("74.00", "10.00");
    }

    @Test
    void exportSectionsSortRowsByIdSoTheBundleIsStable() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isOk());
        for (String source : new String[]{"salary", "rental", "interest"}) {
            mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                            .cookie(AuthFlows.session(session))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":\"1.00\",\"source\":\"" + source + "\"}"))
                    .andExpect(status().isCreated());
        }

        String bundle = mockMvc.perform(get("/api/v1/account/export").cookie(AuthFlows.session(session)))
                .andExpect(jsonPath("$.incomes.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        var incomes = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bundle).path("incomes");
        String firstId = incomes.get(0).path("id").asText();
        String secondId = incomes.get(1).path("id").asText();
        assertThat(firstId.compareTo(secondId)).as("rows sorted by id").isNegative();
    }
}
