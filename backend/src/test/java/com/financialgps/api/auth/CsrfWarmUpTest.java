package com.financialgps.api.auth;

import com.financialgps.application.account.AccountView;
import com.financialgps.application.account.AuthenticateOwnerService;
import com.financialgps.application.account.RegisterOwnerService;
import com.financialgps.api.common.ProblemDetailAdvice;
import com.financialgps.platform.security.SecurityConfig;
import com.financialgps.platform.security.SessionAuthenticator;
import com.financialgps.testsupport.AuthFlows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T002 — CSRF warm-up handshake: the SPA can obtain {@code XSRF-TOKEN} unauthenticated and
 * idempotently; state-changing requests without {@code X-XSRF-TOKEN} fail with 403 CSRF_INVALID;
 * with the double-submit cookie + header the request passes the CSRF filter.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ProblemDetailAdvice.class})
class CsrfWarmUpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterOwnerService registerOwnerService;

    @MockBean
    private AuthenticateOwnerService authenticateOwnerService;

    @MockBean
    private SessionAuthenticator sessionAuthenticator;

    @Test
    void anonymousWarmUpSeedsXsrfCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }

    @Test
    void warmUpIsIdempotent() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/v1/auth/csrf"))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("XSRF-TOKEN"));
        }
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void loginWithDoubleSubmitCookieAndHeaderPassesTheCsrfFilter() throws Exception {
        when(authenticateOwnerService.authenticate(anyString(), anyString()))
                .thenReturn(new AccountView(UUID.randomUUID(), "user@example.com", Instant.now()));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, AuthFlows.login("user@example.com", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }
}
