package com.financialgps.platform.security;

import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T019 — uniform security errors: 401 AUTH_REQUIRED on protected routes without a session; 403
 * CSRF_INVALID on state-changing requests without the XSRF header — same body shape everywhere.
 */
class SecurityErrorSweepTest extends IntegrationTestBase {

    @Test
    void unauthenticatedProtectedRoutesAnswerUniform401() throws Exception {
        String[][] routes = {
                {"GET", "/api/v1/account/me"},
                {"GET", "/api/v1/account/export"},
                {"GET", "/api/test/owned"},
        };
        for (String[] route : routes) {
            mockMvc.perform(get(route[1]))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
                    .andExpect(jsonPath("$.title").value("Authentication required"));
        }
    }

    @Test
    void stateChangingRequestsWithoutCsrfTokenAnswerUniform403() throws Exception {
        String[][] routes = {
                {"/api/v1/auth/login", "{\"email\":\"x@example.com\",\"password\":\"password123\"}"},
                {"/api/v1/auth/register", "{\"email\":\"x@example.com\",\"password\":\"password123\"}"},
                {"/api/v1/auth/logout", ""},
                {"/api/test/owned", "{\"label\":\"x\"}"},
        };
        for (String[] route : routes) {
            mockMvc.perform(post(route[0]).contentType(MediaType.APPLICATION_JSON).content(route[1]))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        }
        mockMvc.perform(delete("/api/v1/account").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }
}
