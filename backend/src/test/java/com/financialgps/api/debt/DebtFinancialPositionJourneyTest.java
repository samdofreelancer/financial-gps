package com.financialgps.api.debt;

import com.financialgps.testsupport.AuthFlows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 002 T016 — end-to-end acceptance over HTTP against the real database, focused on the integration
 * the feature exists for: <strong>Debt → Financial Position</strong>.
 *
 * <pre>
 * register/login → set income → create debt → verify DTI → verify payoff projection
 *   → verify blocked debt behaviour → fix payment → verify blocker clears
 *   → archive debt → verify the Financial Position stops charging the mandatory payment
 * </pre>
 *
 * <p>Every number is asserted from the server response — no client-side arithmetic.
 */
class DebtFinancialPositionJourneyTest extends DebtApiJourneyBase {

    private static final String PROFILE = "{\"currency\":\"VND\",\"savingsAmount\":\"0.00\","
            + "\"emergencyFundAmount\":\"0.00\",\"dependentsCount\":0}";

    private void putProfile(String session) throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isOk());
    }

    private void addIncome(String session, String amount) throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\",\"source\":\"salary\"}"))
                .andExpect(status().isCreated());
    }

    private void addExpense(String session, String amount) throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/expenses"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\",\"category\":\"living\","
                                + "\"expenseType\":\"FIXED\"}"))
                .andExpect(status().isCreated());
    }

    /** Balance 10,000,000 @ 12% has a 100,000 monthly interest: planned 80,000 cannot amortize. */
    private String addBlockedDebt(String session) throws Exception {
        return idOf(postDebt(session, "10000000.00", "50000.00", "80000.00", "0.120000"));
    }

    @Test
    void debtJourney_mandatoryPaymentDrivesTheFinancialPosition() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        var cookie = AuthFlows.session(session);

        // 1. Set the profile and the monthly income/expense the position is built from.
        putProfile(session);
        addIncome(session, "74000000.00");
        addExpense(session, "30000000.00");

        // Before any debt: Mandatory Payment is 0, Net Cash Flow is 74M - 30M = 44M.
        mockMvc.perform(get("/api/v1/profile").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMandatoryPayment.amount").value("0.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("44000000.00"));

        // 2. Create a debt with a 20,000,000 mandatory monthly minimum.
        String debtId = idOf(postDebt(session, "15000000.00", "20000000.00", "20000000.00", "0.180000"));

        // 3. DTI = 20,000,000 / 74,000,000 = 0.2703.
        mockMvc.perform(get("/api/v1/debts/summary").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOutstandingDebt").value("15000000.00"))
                .andExpect(jsonPath("$.totalMinimumMonthlyPayment").value("20000000.00"))
                .andExpect(jsonPath("$.debtToIncome.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.debtToIncome.ratio").value("0.2703"));

        // 4. The single debt carries a deterministic payoff projection.
        mockMvc.perform(get("/api/v1/debts/" + debtId).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.projection.numberOfPayments").value(1));

        // 5. INTEGRATION: the mandatory minimum now reduces Net Cash Flow 74M - 30M - 20M = 24M.
        mockMvc.perform(get("/api/v1/profile").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMandatoryPayment.amount").value("20000000.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("24000000.00"))
                .andExpect(jsonPath("$.availableCapacity.amount").value("24000000.00"));

        // 6. A debt that cannot amortize is BLOCKED and blocks the portfolio — but its contractual
        //    minimum is still a mandatory payment and still reduces cash flow.
        String blockedId = addBlockedDebt(session);
        mockMvc.perform(get("/api/v1/debts/" + blockedId).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection.status").value("BLOCKED"))
                .andExpect(jsonPath("$.projection.reasonCode").value("PAYMENT_DOES_NOT_COVER_INTEREST"));
        mockMvc.perform(get("/api/v1/debts/summary").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioProjection.status").value("BLOCKED"))
                .andExpect(jsonPath("$.portfolioProjection.projectedDebtFreeDate").doesNotExist())
                .andExpect(jsonPath("$.portfolioProjection.reasonCode")
                        .value("PORTFOLIO_CONTAINS_BLOCKED_DEBTS"))
                .andExpect(jsonPath("$.blockedDebtCount").value(1));
        mockMvc.perform(get("/api/v1/profile").cookie(cookie))
                .andExpect(jsonPath("$.totalMandatoryPayment.amount").value("20050000.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("23950000.00"));

        // 7. Fix the planned payment (>= minimum): the individual and portfolio blockers clear.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/debts/" + blockedId))
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(debtBody("10000000.00", "50000.00", "1000000.00", "0.120000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection.status").value("AVAILABLE"));
        mockMvc.perform(get("/api/v1/debts/summary").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioProjection.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.blockedDebtCount").value(0));

        // 8. Archive the second debt: it disappears from every debt view AND from the position.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/debts/" + blockedId)).cookie(cookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/debts/" + blockedId).cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/debts/summary").cookie(cookie))
                .andExpect(jsonPath("$.totalMinimumMonthlyPayment").value("20000000.00"));
        mockMvc.perform(get("/api/v1/profile").cookie(cookie))
                .andExpect(jsonPath("$.totalMandatoryPayment.amount").value("20000000.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("24000000.00"));

        // 9. Archive the last debt: the mandatory burden is gone and the position is restored.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/debts/" + debtId)).cookie(cookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/profile").cookie(cookie))
                .andExpect(jsonPath("$.totalMandatoryPayment.amount").value("0.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("44000000.00"))
                .andExpect(jsonPath("$.availableCapacity.amount").value("44000000.00"));
    }

    @Test
    void unknownOriginalPrincipalStaysUnknown_neverZero() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        var cookie = AuthFlows.session(session);

        // The field is omitted entirely: the server must not invent 0.00 (spec §4.1).
        String created = mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/debts"))
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"creditor\":\"Bank\",\"debtType\":\"PERSONAL_LOAN\","
                                + "\"outstandingBalance\":\"5000000.00\",\"annualInterestRate\":\"0.100000\","
                                + "\"minimumPayment\":\"500000.00\",\"plannedPayment\":\"500000.00\","
                                + "\"dueDay\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalPrincipal").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String id = idOf(created);
        mockMvc.perform(get("/api/v1/debts/" + id).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalPrincipal").doesNotExist());
    }
}
