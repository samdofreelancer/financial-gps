package com.financialgps.api.account;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.login;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T2 endpoint contract: wrong current password → 401 INVALID_CREDENTIALS (identical body to
 * login); weak new password → 422 PASSWORD_POLICY_VIOLATION; success rotates the credential and
 * kills the OTHER session while the caller's own session stays alive.
 */
class ChangePasswordEndpointTest extends IntegrationTestBase {

    private static final String NEXT = "brand new passphrase 2";

    private String body(String current, String next) {
        return "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}";
    }

    private String loginSecondSession(String email, String password) throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION").getValue();
    }

    @Test
    void wrongCurrentPasswordIs401InvalidCredentials() throws Exception {
        String email = uniqueEmail();
        String sessionA = register(mockMvc, email, PASSWORD);

        String body = mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/account/password")
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("wrong password 99", NEXT))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        String loginBody = mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, "wrong password 99")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        // Anti-enumeration: the security-relevant fields are identical to a login failure
        // (only RFC-9457 `instance` differs — it always echoes the request path).
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<?, ?> changeNode = mapper.readValue(body, java.util.Map.class);
        java.util.Map<?, ?> loginNode = mapper.readValue(loginBody, java.util.Map.class);
        org.assertj.core.api.Assertions.assertThat(changeNode.get("code")).isEqualTo(loginNode.get("code"));
        org.assertj.core.api.Assertions.assertThat(changeNode.get("title")).isEqualTo(loginNode.get("title"));
        org.assertj.core.api.Assertions.assertThat(changeNode.get("detail")).isEqualTo(loginNode.get("detail"));
    }

    @Test
    void weakNewPasswordIs422AndOldPasswordStillWorks() throws Exception {
        String email = uniqueEmail();
        String sessionA = register(mockMvc, email, PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/account/password")
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PASSWORD, "short1"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"));

        // Nothing rotated: the old password still signs in.
        loginSecondSession(email, PASSWORD);
    }

    @Test
    void successRotatesCredentialAndKillsOnlyOtherSessions() throws Exception {
        String email = uniqueEmail();
        String sessionA = register(mockMvc, email, PASSWORD);
        String sessionB = loginSecondSession(email, PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/account/password")
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PASSWORD, NEXT))))
                .andExpect(status().isNoContent());

        // Old password dead, new password alive.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        loginSecondSession(email, NEXT);

        // Caller survives, the other device is signed out.
        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionB)))
                .andExpect(status().isUnauthorized());
    }
}
