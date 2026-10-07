package com.financialgps.api.profile;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plan Phase 0 step 3 / Phase 3 step 2 — date BEHAVIOUR at the HTTP contract.
 *
 * <p>The evaluated date is the server business date supplied by the {@code BusinessDate} output port:
 * the client sends no date, and a line recorded through the API is effective immediately, so it
 * counts in the position read back in the same session. This is the end-to-end proof that the
 * business date moved out of the controllers and stayed controllable behind a port.
 */
class ProfileBusinessDateTest extends IntegrationTestBase {

    private static final String PROFILE = "{\"currency\":\"VND\",\"savingsAmount\":\"0.00\","
            + "\"emergencyFundAmount\":\"0.00\",\"dependentsCount\":0}";

    @Test
    void theEvaluatedDateIsTheServerBusinessDateAndNewLinesAreEffectiveImmediately() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String today = LocalDate.now().toString();

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(today));

        // A line recorded now carries the business date as its effective-from date, so it is part of
        // the position the very next read reports.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"74.00\",\"source\":\"salary\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(today))
                .andExpect(jsonPath("$.totalIncome.amount").value("74.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("74.00"));
    }

    @Test
    void aClientSuppliedDateIsNotBoundSoItCannotBackdateOrForwardDateALine() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String today = LocalDate.now().toString();

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isOk());

        // effectiveFrom = year 2000 would still be effective today, so it must not be needed; a
        // future date would silently hide the line, which is exactly what must not happen.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"50.00\",\"source\":\"salary\","
                                + "\"effectiveFrom\":\"2999-01-01\",\"asOf\":\"1999-01-01\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(today))
                .andExpect(jsonPath("$.totalIncome.amount").value("50.00"));
    }
}
