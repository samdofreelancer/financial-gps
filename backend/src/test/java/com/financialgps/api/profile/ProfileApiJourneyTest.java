package com.financialgps.api.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 001 TDD RED (Slice 4 / C1): the HTTP contract of the Financial Profile — happy path, validation
 * (400 VALIDATION_FAILED), authentication (401 AUTH_REQUIRED), not-found / cross-owner
 * (404 RESOURCE_NOT_FOUND) and DTO no-leak checks.
 */
class ProfileApiJourneyTest extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    private static String profileBody(String savings, String emergency, int dependents) {
        return "{\"currency\":\"VND\",\"savingsAmount\":\"" + savings
                + "\",\"emergencyFundAmount\":\"" + emergency
                + "\",\"dependentsCount\":" + dependents + "}";
    }

    private String putProfile(String session, String savings, String emergency, int dependents)
            throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody(savings, emergency, dependents)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String postIncome(String session, String amount, String source) throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\",\"source\":\"" + source + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String postExpense(String session, String amount, String category, String type)
            throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/expenses"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\",\"category\":\"" + category
                                + "\",\"expenseType\":\"" + type + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static String idOf(String body) throws Exception {
        return JSON.readTree(body).path("id").asText();
    }

    @Test
    void unauthenticatedProfileReadIs401() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void unauthenticatedProfileMutationIs401() throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"1.00\",\"source\":\"salary\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void fullJourney_totalsAreServerCalculatedAfterEveryMutation() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);

        // PUT /profile acts as create-or-replace and answers with the saved representation.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("10.00", "5.00", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("VND"))
                .andExpect(jsonPath("$.savingsAmount").value("10.00"))
                .andExpect(jsonPath("$.emergencyFundAmount").value("5.00"))
                .andExpect(jsonPath("$.dependentsCount").value(2))
                .andExpect(jsonPath("$.totalIncome.amount").value("0.00"))
                .andExpect(jsonPath("$.totalIncome.provenance").value("calculated"));

        // Line mutations answer with the saved line; the position is read back from the server.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"74.00\",\"source\":\"salary\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value("74.00"))
                .andExpect(jsonPath("$.source").value("salary"))
                .andExpect(jsonPath("$.id").isNotEmpty());

        String expenseId = idOf(postExpense(session, "30.00", "rent", "FIXED"));

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.amount").value("74.00"))
                .andExpect(jsonPath("$.totalExpenses.amount").value("30.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("44.00"))
                .andExpect(jsonPath("$.availableCapacity.amount").value("44.00"))
                .andExpect(jsonPath("$.incomes[0].provenance").value("actual"))
                .andExpect(jsonPath("$.provenance.length()").value(4))
                .andExpect(jsonPath("$.provenance[0].field").value("Income"))
                .andExpect(jsonPath("$.provenance[0].kind").value("calculated"))
                .andExpect(jsonPath("$.asOf").isNotEmpty());

        // Hard delete: the position immediately reflects the removal.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/expenses/" + expenseId))
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses.amount").value("0.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("74.00"))
                .andExpect(jsonPath("$.expenses.length()").value(0));
    }

    @Test
    void putProfileReplacesRatherThanDuplicating() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);

        putProfile(session, "10.00", "5.00", 2);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("99.00", "1.00", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savingsAmount").value("99.00"))
                .andExpect(jsonPath("$.emergencyFundAmount").value("1.00"))
                .andExpect(jsonPath("$.dependentsCount").value(0));
    }

    @Test
    void updatingAnIncomeReplacesTheAmountAndRecalculatesThePosition() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        putProfile(session, "0.00", "0.00", 0);
        String incomeId = idOf(postIncome(session, "74.00", "salary"));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/incomes/" + incomeId))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"80.00\",\"source\":\"salary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("80.00"));

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(jsonPath("$.totalIncome.amount").value("80.00"))
                .andExpect(jsonPath("$.netCashFlow.amount").value("80.00"));
    }

    @Test
    void negativeNetCashFlowIsReportedNotConcealed() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        putProfile(session, "0.00", "0.00", 0);
        postIncome(session, "20.00", "salary");
        postExpense(session, "30.00", "rent", "VARIABLE");

        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netCashFlow.amount").value("-10.00"))
                .andExpect(jsonPath("$.availableCapacity.amount").value("0.00"));
    }

    @Test
    void invalidBodiesAreValidationFailed() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        putProfile(session, "0.00", "0.00", 0);

        String[][] invalidIncomes = {
                {"{\"amount\":\"-5.00\",\"source\":\"salary\"}", "negative amount"},
                {"{\"amount\":\"1E+2\",\"source\":\"salary\"}", "non-decimal amount"},
                {"{\"amount\":\"10.123\",\"source\":\"salary\"}", "too many decimals"},
                {"{\"amount\":\"999999999999999999.99\",\"source\":\"salary\"}", "beyond numeric(19,2)"},
                {"{\"amount\":\"10.00\",\"source\":\"   \"}", "blank source"},
                {"{\"source\":\"salary\"}", "missing amount"},
                {"{\"amount\":\"abc\",\"source\":\"salary\"}", "non-numeric amount"},
        };
        for (String[] invalid : invalidIncomes) {
            mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                            .cookie(AuthFlows.session(session))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid[0]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        String[][] invalidExpenses = {
                {"{\"amount\":\"1.00\",\"category\":\"rent\",\"expenseType\":\"WEEKLY\"}", "bad expense type"},
                {"{\"amount\":\"1.00\",\"category\":\"  \",\"expenseType\":\"FIXED\"}", "blank category"},
        };
        for (String[] invalid : invalidExpenses) {
            mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/expenses"))
                            .cookie(AuthFlows.session(session))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid[0]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("-1.00", "0.00", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("0.00", "0.00", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"VN\",\"savingsAmount\":\"0.00\","
                                + "\"emergencyFundAmount\":\"0.00\",\"dependentsCount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void crossOwnerLinesAreIndistinguishableFromMissing() throws Exception {
        String sessionA = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        putProfile(sessionA, "0.00", "0.00", 0);
        putProfile(sessionB, "0.00", "0.00", 0);

        String incomeOfA = idOf(postIncome(sessionA, "50.00", "salary"));
        String expenseOfA = idOf(postExpense(sessionA, "5.00", "rent", "FIXED"));
        postIncome(sessionB, "10.00", "salary");

        // B cannot see A's lines.
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(sessionB)))
                .andExpect(jsonPath("$.totalIncome.amount").value("10.00"))
                .andExpect(jsonPath("$.incomes.length()").value(1));

        // B cannot update or delete A's lines — and the answer is identical to "not found".
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/incomes/" + incomeOfA))
                        .cookie(AuthFlows.session(sessionB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"99.00\",\"source\":\"hijack\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Resource not found."));
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

        // A's data is untouched.
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(sessionA)))
                .andExpect(jsonPath("$.totalIncome.amount").value("50.00"))
                .andExpect(jsonPath("$.totalExpenses.amount").value("5.00"));

        // Missing identifiers behave exactly like cross-owner ones.
        String missing = java.util.UUID.randomUUID().toString();
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/incomes/" + missing))
                        .cookie(AuthFlows.session(sessionB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Resource not found."));
    }

    @Test
    void forgedOwnerOrAccountIdentifiersInTheBodyAreIgnored() throws Exception {
        String emailA = AuthFlows.uniqueEmail();
        String emailB = AuthFlows.uniqueEmail();
        String sessionA = AuthFlows.register(mockMvc, emailA, AuthFlows.PASSWORD);
        String sessionB = AuthFlows.register(mockMvc, emailB, AuthFlows.PASSWORD);
        putProfile(sessionA, "0.00", "0.00", 0);
        putProfile(sessionB, "0.00", "0.00", 0);
        String ownerB = jdbc.queryForObject("select id from account where email = ?", String.class, emailB);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"50.00\",\"source\":\"salary\",\"ownerId\":\"" + ownerB
                                + "\",\"accountId\":\"" + ownerB
                                + "\",\"profileId\":\"" + java.util.UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"VND\",\"savingsAmount\":\"7.00\","
                                + "\"emergencyFundAmount\":\"0.00\",\"dependentsCount\":0,"
                                + "\"ownerId\":\"" + ownerB + "\"}"))
                .andExpect(status().isOk());

        // The forged owner sees nothing: the session owner owns every write.
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(sessionB)))
                .andExpect(jsonPath("$.savingsAmount").value("0.00"))
                .andExpect(jsonPath("$.incomes.length()").value(0))
                .andExpect(jsonPath("$.totalIncome.amount").value("0.00"));
        mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(sessionA)))
                .andExpect(jsonPath("$.savingsAmount").value("7.00"))
                .andExpect(jsonPath("$.totalIncome.amount").value("50.00"));
    }

    @Test
    void responsesLeakNoOwnerOrPersistenceIdentifiers() throws Exception {
        String session = AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
        putProfile(session, "10.00", "5.00", 2);
        String created = postIncome(session, "74.00", "salary");
        postExpense(session, "30.00", "rent", "FIXED");
        String profile = mockMvc.perform(get("/api/v1/profile").cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(created).doesNotContain("ownerId", "accountId", "profileId", "password");
        assertThat(profile).doesNotContain("ownerId", "accountId", "profileId", "password")
                .contains("totalIncome", "provenance");
    }
}