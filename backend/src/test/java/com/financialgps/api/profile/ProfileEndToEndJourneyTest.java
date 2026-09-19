package com.financialgps.api.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
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
 * 001 Slice 7 / V3 — the approved end-to-end Financial Profile journey (spec US1 + US2):
 * register → csrf → profile → incomes/expenses → server position → edit → recompute →
 * untouched future-feature sections → logout isolation → persisted state after re-login.
 */
class ProfileEndToEndJourneyTest extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    private String line(String session, String path, String body) throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post(path))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void completeFinancialProfileJourney() throws Exception {
        String email = AuthFlows.uniqueEmail();
        String password = AuthFlows.PASSWORD;
        String session = AuthFlows.register(mockMvc, email, password);

        // US1: record the current position.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"VND\",\"savingsAmount\":\"100.00\","
                                + "\"emergencyFundAmount\":\"50.00\",\"dependentsCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.amount").value("0.00"));

        String salary = JSON.readTree(line(session, "/api/v1/incomes",
                "{\"amount\":\"74.00\",\"source\":\"salary\"}")).path("id").asText();
        line(session, "/api/v1/incomes", "{\"amount\":\"6.00\",\"source\":\"rental\"}");
        line(session, "/api/v1/expenses",
                "{\"amount\":\"30.00\",\"category\":\"rent\",\"expenseType\":\"FIXED\"}");
        line(session, "/api/v1/expenses",
                "{\"amount\":\"20.00\",\"category\":\"food\",\"expenseType\":\"VARIABLE\"}");

        // Position: Income 80, Expense 50, Net Cash Flow 30, Available Capacity 30.
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.amount").value("80.00"))
                .andExpect(jsonPath("$.totalExpenses.amount").value("50.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("30.00"))
                .andExpect(jsonPath("$.availableCapacity.amount").value("30.00"))
                .andExpect(jsonPath("$.savingsAmount").value("100.00"))
                .andExpect(jsonPath("$.emergencyFundAmount").value("50.00"))
                .andExpect(jsonPath("$.dependentsCount").value(2));

        // US2: facts (actual) are distinguishable from totals (calculated).
        String view = mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andReturn().getResponse().getContentAsString();
        var tree = JSON.readTree(view);
        assertThat(tree.path("incomes").get(0).path("provenance").asText()).isEqualTo("actual");
        assertThat(tree.path("totalIncome").path("provenance").asText()).isEqualTo("calculated");
        assertThat(tree.path("provenance")).hasSize(4);

        // Edit an amount: the server totals follow.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/incomes/" + salary))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"94.00\",\"source\":\"salary\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(jsonPath("$.totalIncome.amount").value("100.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("50.00"));

        // Profile edits never touch future features (002/004-006 sections stay empty in the export).
        String export = mockMvc.perform(get("/api/v1/account/export").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debts.length()").value(0))
                .andExpect(jsonPath("$.goals.length()").value(0))
                .andExpect(jsonPath("$.timelineChanges.length()").value(0))
                .andExpect(jsonPath("$.allocationRules.length()").value(0))
                .andExpect(jsonPath("$.gpsSnapshots.length()").value(0))
                .andExpect(jsonPath("$.reviewLedger.length()").value(0))
                .andReturn().getResponse().getContentAsString();
        assertThat(export).as("001 changes only profile-owned sections")
                .doesNotContain("ownerId", "accountId");

        // Logout terminates the session server-side.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/auth/logout"))
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        // A fresh login returns the persisted position (FR-004: values are preserved).
        String reLogin = mockMvc.perform(AuthFlows.withCsrf(mockMvc, AuthFlows.login(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION").getValue();
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(reLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.amount").value("100.00"))
                .andExpect(jsonPath("$.totalExpenses.amount").value("50.00"));

        // Deleting a line keeps the truth: a negative Net Cash Flow is reported, capacity clamps.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/incomes/" + salary))
                        .cookie(AuthFlows.session(reLogin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(reLogin)))
                .andExpect(jsonPath("$.totalIncome.amount").value("6.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("-44.00"))
                .andExpect(jsonPath("$.availableCapacity.amount").value("0.00"));
    }
}