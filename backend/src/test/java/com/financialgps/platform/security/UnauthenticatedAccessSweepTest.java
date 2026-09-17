package com.financialgps.platform.security;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.financialgps.testsupport.AuthFlows.login;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T001 — route rules baseline: every protected route answers 401 AUTH_REQUIRED without a session;
 * only the three handshake routes are public (FR-006, SC-005).
 */
class UnauthenticatedAccessSweepTest extends IntegrationTestBase {

    private static final String[] PROTECTED_GETS = {
            "/api/v1/account/me",
            "/api/v1/account/export",
            "/api/v1/profile",
            "/api/test/owned",
    };

    @Test
    void protectedGetRoutesRequireAuthentication() throws Exception {
        for (String route : PROTECTED_GETS) {
            mockMvc.perform(get(route))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        }
    }

    @Test
    void protectedStateChangingRoutesRequireAuthentication() throws Exception {
        String income = "{\"amount\":\"1.00\",\"source\":\"salary\"}";
        String expense = "{\"amount\":\"1.00\",\"category\":\"rent\",\"expenseType\":\"FIXED\"}";
        String profile = "{\"currency\":\"VND\",\"savingsAmount\":\"1.00\","
                + "\"emergencyFundAmount\":\"1.00\",\"dependentsCount\":0}";
        String missingId = java.util.UUID.randomUUID().toString();
        MockHttpServletRequestBuilder[] requests = {
                delete("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\"}"),
                post("/api/v1/auth/logout"),
                post("/api/test/owned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"nope\"}"),
                put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profile),
                post("/api/v1/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(income),
                put("/api/v1/incomes/" + missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(income),
                delete("/api/v1/incomes/" + missingId),
                post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expense),
                put("/api/v1/expenses/" + missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expense),
                delete("/api/v1/expenses/" + missingId),
        };
        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(AuthFlows.withCsrf(mockMvc, request))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        }
    }

    @Test
    void handshakeRoutesAreReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk());

        // CSRF still applies to the POST handshakes: without the header they answer 403, never 401.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void registerAndLoginWorkEndToEndWithCsrf() throws Exception {
        String email = uniqueEmail();
        register(mockMvc, email, AuthFlows.PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, AuthFlows.PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
