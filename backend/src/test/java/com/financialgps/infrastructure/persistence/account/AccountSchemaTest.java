package com.financialgps.infrastructure.persistence.account;

import com.financialgps.infrastructure.persistence.ownership.OwnershipQueries;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T004 — schema contract: case-insensitive uniqueness, display case preserved, session tables. */
class AccountSchemaTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OwnershipQueries ownershipQueries;

    @Test
    void lowerEmailUniquenessIsEnforcedWhileDisplayCaseIsPreserved() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into account (id, email, password_hash, role) values (?, ?, ?, ?)",
                id, "User@Example.com", "$2a$12$fixturehash", "OWNER");

        String stored = jdbc.queryForObject("select email from account where id = ?", String.class, id);
        assertThat(stored).isEqualTo("User@Example.com");

        assertThatThrownBy(() -> jdbc.update(
                        "insert into account (id, email, password_hash, role) values (?, ?, ?, ?)",
                        UUID.randomUUID(), "user@example.com", "$2a$12$fixturehash", "OWNER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void springSessionJdbcSchemaExists() {
        // PostgreSQL folds unquoted identifiers to lowercase in information_schema.
        List<String> tables = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = current_schema()
                  and table_name in ('spring_session', 'spring_session_attributes')
                """, String.class);
        assertThat(tables).containsExactlyInAnyOrder("spring_session", "spring_session_attributes");
    }

    @Test
    void ownershipRegistryDiscoversOwnerScopedTables() {
        assertThat(ownershipQueries.ownerScopedTables())
                .contains("test_owned_resource", "test_owned_archive");
        assertThat(ownershipQueries.countRowsForOwner("test_owned_resource", UUID.randomUUID()))
                .isZero();
    }
}
