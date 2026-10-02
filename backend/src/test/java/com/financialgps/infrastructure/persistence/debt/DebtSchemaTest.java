package com.financialgps.infrastructure.persistence.debt;

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

/** T005 RED: V3__debt.sql contract — types, CHECKs, indexes, cascade, ownership registry. */
class DebtSchemaTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OwnershipQueries ownershipQueries;

    private UUID newOwner() {
        UUID ownerId = UUID.randomUUID();
        jdbc.update("insert into account (id, email, password_hash, role) values (?, ?, ?, ?)",
                ownerId, "debt-schema-" + ownerId + "@example.com", "$2a$12$fixturehash", "OWNER");
        return ownerId;
    }

    private void insertDebt(UUID id, UUID ownerId, String status) {
        jdbc.update("""
                insert into debt (id, owner_id, creditor, debt_type, original_principal,
                  outstanding_balance, annual_interest_rate, minimum_payment, planned_payment, due_day, status)
                values (?, ?, 'Bank', 'CREDIT_CARD', 20000000.00, 15000000.00, 0.180000, 1500000.00, 3000000.00, 15, ?)
                """, id, ownerId, status);
    }

    @Test
    void migrationAppliesV3WithMoneyAndRateColumns() {
        Long applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '3'", Long.class);
        assertThat(applied).as("V3__debt.sql is applied after V2").isEqualTo(1L);

        for (String column : List.of("outstanding_balance", "minimum_payment", "planned_payment")) {
            var row = jdbc.queryForMap("""
                    select numeric_precision, numeric_scale, is_nullable
                    from information_schema.columns
                    where table_schema = current_schema() and table_name = 'debt' and column_name = ?
                    """, column);
            assertThat(row.get("numeric_precision")).as("%s precision", column).isEqualTo(19);
            assertThat(row.get("numeric_scale")).as("%s scale", column).isEqualTo(2);
            assertThat(row.get("is_nullable")).as("%s nullability", column).isEqualTo("NO");
        }
        var rate = jdbc.queryForMap("""
                select numeric_precision, numeric_scale, is_nullable
                from information_schema.columns
                where table_schema = current_schema() and table_name = 'debt' and column_name = 'annual_interest_rate'
                """);
        assertThat(rate.get("numeric_precision")).isEqualTo(9);
        assertThat(rate.get("numeric_scale")).isEqualTo(6);
        assertThat(rate.get("is_nullable")).isEqualTo("YES");
    }

    @Test
    void checkConstraintsRejectNegativeMoneyAndInvertedPayments() {
        UUID ownerId = newOwner();
        assertThatThrownBy(() -> jdbc.update("""
                insert into debt (id, owner_id, creditor, debt_type, outstanding_balance, minimum_payment, planned_payment, status)
                values (?, ?, 'B', 'OTHER', -1.00, 1.00, 1.00, 'ACTIVE')
                """, UUID.randomUUID(), ownerId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into debt (id, owner_id, creditor, debt_type, outstanding_balance, minimum_payment, planned_payment, status)
                values (?, ?, 'B', 'OTHER', 100.00, 200.00, 100.00, 'ACTIVE')
                """, UUID.randomUUID(), ownerId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into debt (id, owner_id, creditor, debt_type, outstanding_balance, minimum_payment, planned_payment, status)
                values (?, ?, 'B', 'OTHER', 100.00, 10.00, 10.00, 'BOGUS')
                """, UUID.randomUUID(), ownerId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void indexesExistAndOwnershipRegistryDiscoversDebt() {
        List<String> indexes = jdbc.queryForList("""
                select indexname from pg_indexes
                where schemaname = current_schema() and indexname in ('ix_debt_owner', 'ix_debt_owner_status')
                """, String.class);
        assertThat(indexes).containsExactlyInAnyOrder("ix_debt_owner", "ix_debt_owner_status");
        assertThat(ownershipQueries.ownerScopedTables()).contains("debt");
    }

    @Test
    void accountDeletionCascadesWithZeroOrphans() {
        UUID ownerId = newOwner();
        insertDebt(UUID.randomUUID(), ownerId, "ACTIVE");
        jdbc.update("delete from account where id = ?", ownerId);
        assertThat(ownershipQueries.countRowsForOwner("debt", ownerId)).isZero();
    }
}
