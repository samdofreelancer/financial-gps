package com.financialgps.api.account;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T3: re-authentication for the two destructive/sensitive account endpoints. No credential proof →
 * rejected (400 VALIDATION_FAILED); wrong password → 401 INVALID_CREDENTIALS; correct password →
 * the operation proceeds exactly as before.
 */
class AccountReauthTest extends IntegrationTestBase {

    @Test
    void exportWithoutReauthHeaderIsRejected() throws Exception {
        String sessionValue = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(get("/api/v1/account/export").cookie(session(sessionValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void exportWithWrongPasswordIs401InvalidCredentials() throws Exception {
        String sessionValue = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(get("/api/v1/account/export")
                        .cookie(session(sessionValue))
                        .header("X-Reauth-Password", "wrong password 99"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void exportWithCorrectPasswordPasses() throws Exception {
        String sessionValue = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(get("/api/v1/account/export")
                        .cookie(session(sessionValue))
                        .header("X-Reauth-Password", PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void deleteWithoutPasswordIsRejected() throws Exception {
        String sessionValue = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/account")
                        .cookie(session(sessionValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteWithWrongPasswordIs401InvalidCredentials() throws Exception {
        String sessionValue = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/account")
                        .cookie(session(sessionValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\",\"password\":\"wrong password 99\"}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void deleteWithCorrectPasswordStillDeletes() throws Exception {
        String sessionValue = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/account")
                        .cookie(session(sessionValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\",\"password\":\"" + PASSWORD + "\"}")))
                .andExpect(status().isNoContent());
    }
}
