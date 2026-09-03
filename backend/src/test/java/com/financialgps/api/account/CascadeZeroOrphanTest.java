package com.financialgps.api.account;

import com.financialgps.infrastructure.persistence.ownership.OwnershipQueries;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

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
 * T018 — FR-012/FR-014, SC-007/SC-008: confirmed hard delete removes the account row and every
 * owned row (resource + archive shapes) via FK cascade; zero orphans in every owner-scoped table;
 * other owners and their data are untouched.
 */
class CascadeZeroOrphanTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OwnershipQueries ownershipQueries;

    @Test
    void confirmedDeleteCascadesToEveryOwnedTableWithZeroOrphans() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        String sessionA = register(mockMvc, emailA, PASSWORD);
        String sessionB = register(mockMvc, emailB, PASSWORD);
        UUID ownerIdA = jdbc.queryForObject("select id from account where email = ?", UUID.class, emailA);
        UUID ownerIdB = jdbc.queryForObject("select id from account where email = ?", UUID.class, emailB);

        // Seed owned rows for A (active resource + archive shape) and B (control).
        jdbc.update("insert into test_owned_archive (id, owner_id, payload) values (?, ?, ?)",
                UUID.randomUUID(), ownerIdA, "a-ledger-1");
        jdbc.update("insert into test_owned_archive (id, owner_id, payload) values (?, ?, ?)",
                UUID.randomUUID(), ownerIdA, "a-ledger-2");
        jdbc.update("insert into test_owned_archive (id, owner_id, payload) values (?, ?, ?)",
                UUID.randomUUID(), ownerIdB, "b-ledger-1");
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/test/owned"))
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"a-resource\"}"))
                .andExpect(status().isCreated());

        // Confirmation gate.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/account"))
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"delete\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUIRED"));
        assertThat(jdbc.queryForObject("select count(*) from account where id = ?", Long.class, ownerIdA))
                .isEqualTo(1L);

        // Confirmed delete → 204, account row gone, cascade fired.
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/account"))
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"DELETE\"}"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from account where id = ?", Long.class, ownerIdA))
                .isZero();

        // Zero orphans in EVERY owner-scoped table (registry/information_schema-driven, FR-014).
        List<String> ownedTables = ownershipQueries.ownerScopedTables();
        assertThat(ownedTables).contains("test_owned_resource", "test_owned_archive");
        for (String table : ownedTables) {
            assertThat(ownershipQueries.countRowsForOwner(table, ownerIdA))
                    .as("zero orphans in %s", table).isZero();
        }

        // SC-008: owner B and their data are untouched; A's session is dead.
        assertThat(ownershipQueries.countRowsForOwner("test_owned_archive", ownerIdB)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from account where id = ?", Long.class, ownerIdB))
                .isEqualTo(1L);
        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionA)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/account/me").cookie(session(sessionB)))
                .andExpect(status().isOk());
    }

    @Test
    void deletedOwnerCannotBeUsedForLogin() throws Exception {
        String email = uniqueEmail();
        String sessionA = register(mockMvc, email, PASSWORD);
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, delete("/api/v1/account"))
                        .cookie(session(sessionA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"DELETE\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, login(email, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
