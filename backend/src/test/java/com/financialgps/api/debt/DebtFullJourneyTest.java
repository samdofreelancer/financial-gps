package com.financialgps.api.debt;

import com.financialgps.testsupport.AuthFlows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T011 RED: full debt lifecycle over HTTP against the real database. */
class DebtFullJourneyTest extends DebtApiJourneyBase {

    @Test
    void fullJourney_createListSummaryUpdatePayoffArchive() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        var cookie = AuthFlows.session(session);

        String idA = idOf(postDebt(session, "1000.00", "50.00", "100.00", "0.120000"));
        String idB = idOf(postDebt(session, "2000.00", "200.00", "200.00", "0.000000"));

        mockMvc.perform(get("/api/v1/debts").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/debts/summary").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOutstandingDebt").value("3000.00"))
                .andExpect(jsonPath("$.totalMinimumMonthlyPayment").value("250.00"))
                .andExpect(jsonPath("$.portfolioProjection.status").value("AVAILABLE"));

        mockMvc.perform(get("/api/v1/debts/" + idA).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.projection.numberOfPayments").value(11));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/debts/" + idB))
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(debtBody("0.00", "0.00", "0.00", "0.000000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID_OFF"));

        mockMvc.perform(get("/api/v1/debts/summary").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOutstandingDebt").value("1000.00"));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/debts/" + idB)).cookie(cookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/debts/" + idB).cookie(cookie))
                .andExpect(status().isNotFound());
    }
}
