package com.financialgps.api.auth;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.login;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T011 — idle expiry (FR-009, SC-004): with a 2-second idle timeout, a session past the timeout
 * is rejected and re-login works.
 */
@TestPropertySource(properties = {"financial.auth.session-idle-timeout=PT2S"})
class IdleSessionExpiryTest extends IntegrationTestBase {
    @Test
    void sessionPastIdleTimeoutIsRejectedAndReLoginWorks() throws Exception {
        String email = uniqueEmail();
        String sessionId = register(mockMvc, email, PASSWORD);

        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionId)))
                .andExpect(status().isOk());

        Thread.sleep(2500); // 2s timeout + margin

        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        String freshSession = mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION").getValue();

        mockMvc.perform(get("/api/v1/account/me").cookie(session(freshSession)))
                .andExpect(status().isOk());
    }
}
