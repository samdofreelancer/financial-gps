package com.financialgps.api.auth;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.login;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T009 — session flow: register auto sign-in, session fixation defense (rotated id at login),
 * server-side logout invalidation (FR-002/FR-003), cookie attributes.
 */
class SessionFlowTest extends IntegrationTestBase {

    @Test
    void registerAutoSignInSetsSessionCookie() throws Exception {
        MvcResult result = mockMvc.perform(
                        AuthFlows.withCsrf(mockMvc, AuthFlows.register(uniqueEmail(), PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies)
                .anyMatch(cookie -> cookie.startsWith("SESSION=")
                        && cookie.contains("HttpOnly")
                        && cookie.contains("SameSite=Lax"))
                .as("session cookie must be HttpOnly + SameSite=Lax (plan §Architecture)");
    }

    @Test
    void meReturnsTheSignedInAccount() throws Exception {
        String email = uniqueEmail();
        String sessionId = register(mockMvc, email, PASSWORD);

        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void loginRotatesTheSessionId() throws Exception {
        String email = uniqueEmail();
        String firstSessionId = register(mockMvc, email, PASSWORD);

        // The browser always carries the current session cookie when signing in again.
        String secondSessionId = mockMvc.perform(
                        AuthFlows.withCsrf(mockMvc, login(email, PASSWORD)).cookie(session(firstSessionId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION").getValue();

        assertThat(secondSessionId)
                .as("session id must be rotated at sign-in (fixation defense)")
                .isNotEqualTo(firstSessionId);

        mockMvc.perform(get("/api/v1/account/me").cookie(session(firstSessionId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        mockMvc.perform(get("/api/v1/account/me").cookie(session(secondSessionId)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesTheSessionServerSide() throws Exception {
        String email = uniqueEmail();
        String activeSession = register(mockMvc, email, PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/auth/logout"))
                        .cookie(session(activeSession)))
                .andExpect(status().isNoContent());

        // The old cookie no longer authenticates — the session is dead server-side (FR-003).
        mockMvc.perform(get("/api/v1/account/me").cookie(session(activeSession)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void loginWithWrongPasswordFailsWithUniformBody() throws Exception {
        String email = uniqueEmail();
        register(mockMvc, email, PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, "wrong password 99")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
