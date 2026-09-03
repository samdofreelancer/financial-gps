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
        MockHttpServletRequestBuilder[] requests = {
                delete("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\"}"),
                post("/api/v1/auth/logout"),
                post("/api/test/owned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"nope\"}"),
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
