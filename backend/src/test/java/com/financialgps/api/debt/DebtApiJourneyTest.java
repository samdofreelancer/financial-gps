package com.financialgps.api.debt;

import com.financialgps.testsupport.AuthFlows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T009+T010 RED: debt HTTP contract, validation (400) and cross-owner isolation (404). */
class DebtApiJourneyTest extends DebtApiJourneyBase {

    @Test
    void unauthenticatedDebtReadIs401() throws Exception {
        mockMvc.perform(get("/api/v1/debts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void plannedBelowMinimumIs400() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/debts"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debtBody("1000.00", "100.00", "50.00", "0.120000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void crossOwnerDebtIs404() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String id = idOf(postDebt(sessionA, "1000.00", "50.00", "100.00", "0.120000"));

        mockMvc.perform(get("/api/v1/debts/" + id).cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/debts/" + id))
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound());
    }
}
