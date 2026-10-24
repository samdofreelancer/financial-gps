package com.financialgps.api.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared full-flow seeding and JSON helpers for the Feature 005 API tests (T013, T014, T033).
 *
 * <p>Every test registers its own owner, so owner scoping is never simulated: the roadmap is read
 * through the real session, the real adapters (001 profile, 002 debt, 003 goals) and the real
 * controller. Nothing here asserts a business rule — that is the domain tests' job.
 */
abstract class RoadmapApiSupport extends IntegrationTestBase {

    static final ObjectMapper JSON = new ObjectMapper();

    /**
     * UTC "today" — the same clock the server's {@code BusinessDate} reads (the test JVM and the
     * application both run with {@code user.timezone=UTC}).
     */
    static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

    /**
     * The evaluated date used by the API tests. Feature 001 income/expense rows carry an
     * {@code effectiveFrom} equal to the business date at creation, so an {@code asOf} in the past
     * legitimately reports Available Capacity {@code 0.00}; tomorrow is the nearest deterministic
     * date on which today's seeded facts are effective.
     */
    static final LocalDate AS_OF_DATE = TODAY.plusDays(1);

    static final String AS_OF = AS_OF_DATE.toString();

    /** Exactly 12 monthly periods after {@link #AS_OF_DATE}, so required capacity divides evenly. */
    static final LocalDate TARGET_DATE = AS_OF_DATE.plusMonths(12);

    static final String TARGET = TARGET_DATE.toString();

    String newOwner() throws Exception {
        return AuthFlows.register(mockMvc, AuthFlows.uniqueEmail(), AuthFlows.PASSWORD);
    }

    void putProfile(String session, String currency) throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, put("/api/v1/profile"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"" + currency + "\",\"savingsAmount\":\"0.00\","
                                + "\"emergencyFundAmount\":\"0.00\",\"dependentsCount\":0}"))
                .andExpect(status().isOk());
    }

    void postIncome(String session, String amount) throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/incomes"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\",\"source\":\"salary\"}"))
                .andExpect(status().isCreated());
    }

    String postDebt(String session, String balance, String minimumPayment, String plannedPayment,
                    String annualRate) throws Exception {
        String body = mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/debts"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debtBody(balance, minimumPayment, plannedPayment, annualRate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body).path("id").asText();
    }

    String postGoal(String session, String name, String goalType, String target, String current,
                    String targetDate, String priority) throws Exception {
        String body = mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/goals"))
                        .cookie(AuthFlows.session(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody(name, goalType, target, current, targetDate, priority)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body).path("id").asText();
    }

    JsonNode roadmapJson(String session, String asOf) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/roadmap")
                .cookie(AuthFlows.session(session));
        if (asOf != null) {
            request.param("asOf", asOf);
        }
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body);
    }

    JsonNode debtSummary(String session) throws Exception {
        String body = mockMvc.perform(get("/api/v1/debts/summary")
                        .cookie(AuthFlows.session(session)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(body);
    }

    static String goalBody(String name, String goalType, String target, String current,
                           String targetDate, String priority) {
        return "{\"name\":\"" + name + "\",\"goalType\":\"" + goalType + "\","
                + "\"targetAmount\":" + quoted(target) + ","
                + "\"currentAmount\":" + quoted(current) + ","
                + "\"targetDate\":" + quoted(targetDate) + ","
                + "\"priority\":" + priority + "}";
    }

    static String debtBody(String balance, String minimumPayment, String plannedPayment,
                           String annualRate) {
        return "{\"creditor\":\"Bank\",\"debtType\":\"CREDIT_CARD\","
                + "\"originalPrincipal\":\"" + balance + "\","
                + "\"outstandingBalance\":\"" + balance + "\","
                + "\"annualInterestRate\":" + quoted(annualRate) + ","
                + "\"minimumPayment\":\"" + minimumPayment + "\","
                + "\"plannedPayment\":\"" + plannedPayment + "\",\"dueDay\":15}";
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    static JsonNode stage(JsonNode roadmap, int index) {
        return roadmap.path("stages").get(index);
    }

    static String text(JsonNode node, String field) {
        return node.path(field).asText();
    }

    static Set<String> keysOf(JsonNode node) {
        Set<String> keys = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    /** No key anywhere may claim an allocation/funding/shortfall quantity 005 does not have. */
    static void assertNoAllocationOrFundingKeys(JsonNode node) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name -> assertThat(name.toLowerCase())
                    .doesNotContain("allocation")
                    .doesNotContain("funded")
                    .doesNotContain("shortfall")
                    .doesNotContain("allocationstatus"));
            node.elements().forEachRemaining(RoadmapApiSupport::assertNoAllocationOrFundingKeys);
        } else if (node.isArray()) {
            node.elements().forEachRemaining(RoadmapApiSupport::assertNoAllocationOrFundingKeys);
        }
    }

    static void assertEtasAreHonest(JsonNode roadmap) {
        for (JsonNode stage : roadmap.path("stages")) {
            if (stage.path("projectedCompletionDate").isNull()) {
                assertThat(stage.path("etaReasonCode").isNull())
                        .as("a null projectedCompletionDate must carry an etaReasonCode")
                        .isFalse();
            } else {
                assertThat(stage.path("etaReasonCode").isNull())
                        .as("an available projectedCompletionDate must not carry a reason code")
                        .isTrue();
            }
            assertThat(stage.path("etaAssumption").asText()).isNotEmpty();
            assertThat(stage.path("etaBasis").asText()).isNotEmpty();
        }
    }
}
