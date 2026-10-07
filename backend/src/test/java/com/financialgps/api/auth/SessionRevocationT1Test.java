package com.financialgps.api.auth;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.login;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T1: session revocation. Both logout-all and account deletion must kill EVERY session row of the
 * owner in the SPRING_SESSION store — a stolen cookie from another device must not survive.
 */
class SessionRevocationT1Test extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private String loginSecondSession(String email, String password) throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION").getValue();
    }

    private String ownerIdOf(String sessionValue) throws Exception {
        return mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionValue)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private int sessionRows(String principalName) {
        Integer count = jdbc.queryForObject(
                "select count(*) from spring_session where principal_name = ?", Integer.class, principalName);
        return count == null ? 0 : count;
    }

    @Test
    void logoutAllKillsEverySessionIncludingTheCallers() throws Exception {
        String email = uniqueEmail();
        String sessionA = register(mockMvc, email, PASSWORD);
        String sessionB = loginSecondSession(email, PASSWORD);
        String ownerId = ownerIdOf(sessionA);
        assertThat(sessionRows(ownerId)).isGreaterThanOrEqualTo(2);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/v1/auth/logout-all"))
                        .cookie(session(sessionA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionA)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionB)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        assertThat(sessionRows(ownerId)).isZero();
    }

    @Test
    void accountDeleteKillsEverySession() throws Exception {
        String email = uniqueEmail();
        String sessionA = register(mockMvc, email, PASSWORD);
        String sessionB = loginSecondSession(email, PASSWORD);
        String ownerId = ownerIdOf(sessionA);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/account")
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\",\"password\":\"" + PASSWORD + "\"}")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionA)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionB)))
                .andExpect(status().isUnauthorized());
        assertThat(sessionRows(ownerId)).isZero();
    }
}
