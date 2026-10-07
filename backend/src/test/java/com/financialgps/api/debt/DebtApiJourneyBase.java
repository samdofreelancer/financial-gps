package com.financialgps.api.debt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for debt API journey tests (T009–T011). */
class DebtApiJourneyBase extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    static String debtBody(String balance, String min, String plan, String rate) {
        return "{\"creditor\":\"Bank\",\"debtType\":\"CREDIT_CARD\","
                + "\"originalPrincipal\":\"" + balance + "\",\"outstandingBalance\":\"" + balance + "\","
                + "\"annualInterestRate\":" + (rate == null ? "null" : ("\"" + rate + "\"")) + ","
                + "\"minimumPayment\":\"" + min + "\",\"plannedPayment\":\"" + plan + "\",\"dueDay\":15}";
    }

    static String idOf(String body) throws Exception {
        return JSON.readTree(body).path("id").asText();
    }

    String postDebt(String session, String balance, String min, String plan, String rate)
            throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/debts"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debtBody(balance, min, plan, rate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("VND"))
                .andReturn().getResponse().getContentAsString();
    }
}
