package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.infrastructure.persistence.ownership.OwnershipQueries;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 001 TDD RED (Slice 1 / P1): V2__profile.sql contract — migration order, numeric(19,2) precision,
 * CHECK constraints, UNIQUE owner, FK, cascade, indexes and the ownership registry
 * (implementation-plan §Slice 1).
 */
class ProfileSchemaTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OwnershipQueries ownershipQueries;

    private UUID newOwner() {
        UUID ownerId = UUID.randomUUID();
        jdbc.update("insert into account (id, email, password_hash, role) values (?, ?, ?, ?)",
                ownerId, "schema-" + ownerId + "@example.com", "$2a$12$fixturehash", "OWNER");
        return ownerId;
    }

    private UUID insertProfile(UUID ownerId) {
        UUID profileId = UUID.randomUUID();
        jdbc.update("""
                insert into profile (id, owner_id, currency, savings_amount, emergency_fund_amount, dependents_count)
                values (?, ?, 'VND', 10.00, 5.00, 1)
                """, profileId, ownerId);
        return profileId;
    }

    @Test
    void migrationAppliesV2WithNumeric19_2MoneyColumns() {
        Long applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '2'", Long.class);
        assertThat(applied).as("V2__profile.sql is applied after V1__auth.sql").isEqualTo(1L);

        List<String> moneyColumns = List.of(
                "profile.savings_amount", "profile.emergency_fund_amount",
                "income.amount", "expense.amount");
        for (String column : moneyColumns) {
            String[] parts = column.split("\\.");
            var row = jdbc.queryForMap("""
                    select numeric_precision, numeric_scale, is_nullable
                    from information_schema.columns
                    where table_schema = current_schema() and table_name = ? and column_name = ?
                    """, parts[0], parts[1]);
            assertThat(row.get("numeric_precision")).as("%s precision", column).isEqualTo(19);
            assertThat(row.get("numeric_scale")).as("%s scale", column).isEqualTo(2);
            assertThat(row.get("is_nullable")).as("%s nullability", column).isEqualTo("NO");
        }
    }

    @Test
    void negativeMoneyAndDependentsAreRejectedByCheckConstraints() {
        UUID ownerId = newOwner();

        assertThatThrownBy(() -> jdbc.update("""
                        insert into profile (id, owner_id, currency, savings_amount)
                        values (?, ?, 'VND', -0.01)
                        """, UUID.randomUUID(), ownerId))
                .describedAs("negative savings")
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.update("""
                        insert into profile (id, owner_id, currency, dependents_count)
                        values (?, ?, 'VND', -1)
                        """, UUID.randomUUID(), ownerId))
                .describedAs("negative dependents")
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID profileId = insertProfile(ownerId);
        assertThatThrownBy(() -> jdbc.update("""
                        insert into income (id, owner_id, profile_id, amount, source, active)
                        values (?, ?, ?, -1.00, 'salary', true)
                        """, UUID.randomUUID(), ownerId, profileId))
                .describedAs("negative income amount")
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.update("""
                        insert into expense (id, owner_id, profile_id, amount, category, expense_type, active)
                        values (?, ?, ?, -1.00, 'rent', 'FIXED', true)
                        """, UUID.randomUUID(), ownerId, profileId))
                .describedAs("negative expense amount")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expenseTypeIsConstrainedToFixedOrVariable() {
        UUID ownerId = newOwner();
        UUID profileId = insertProfile(ownerId);

        assertThatThrownBy(() -> jdbc.update("""
                        insert into expense (id, owner_id, profile_id, amount, category, expense_type)
                        values (?, ?, ?, 1.00, 'rent', 'WEEKLY')
                        """, UUID.randomUUID(), ownerId, profileId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oneProfilePerOwnerIsEnforced() {
        UUID ownerId = newOwner();
        insertProfile(ownerId);

        assertThatThrownBy(() -> insertProfile(ownerId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void incomeAndExpenseRequireAnExistingProfileAndOwner() {
        UUID ownerId = newOwner();

        assertThatThrownBy(() -> jdbc.update("""
                        insert into income (id, owner_id, profile_id, amount, source)
                        values (?, ?, ?, 1.00, 'salary')
                        """, UUID.randomUUID(), ownerId, UUID.randomUUID()))
                .describedAs("profile_id must reference an existing profile")
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.update("""
                        insert into profile (id, owner_id, currency)
                        values (?, ?, 'VND')
                        """, UUID.randomUUID(), UUID.randomUUID()))
                .describedAs("owner_id must reference an existing account")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ownerScopedIndexesExist() {
        List<String> indexes = jdbc.queryForList("""
                select indexname from pg_indexes
                where schemaname = current_schema()
                  and indexname in ('ix_profile_owner', 'ix_income_owner', 'ix_expense_owner',
                                    'ix_income_profile', 'ix_expense_profile')
                """, String.class);

        assertThat(indexes).containsExactlyInAnyOrder(
                "ix_profile_owner", "ix_income_owner", "ix_expense_owner",
                "ix_income_profile", "ix_expense_profile");
    }

    @Test
    void ownershipRegistryDiscoversProfileTables() {
        assertThat(ownershipQueries.ownerScopedTables())
                .contains("profile", "income", "expense");
    }

    @Test
    void accountDeletionCascadesThroughProfileAndLinesWithZeroOrphans() {
        UUID ownerId = newOwner();
        UUID profileId = insertProfile(ownerId);
        jdbc.update("""
                insert into income (id, owner_id, profile_id, amount, source)
                values (?, ?, ?, 74.00, 'salary')
                """, UUID.randomUUID(), ownerId, profileId);
        jdbc.update("""
                insert into expense (id, owner_id, profile_id, amount, category, expense_type)
                values (?, ?, ?, 30.00, 'rent', 'FIXED')
                """, UUID.randomUUID(), ownerId, profileId);

        jdbc.update("delete from account where id = ?", ownerId);

        for (String table : List.of("profile", "income", "expense")) {
            assertThat(ownershipQueries.countRowsForOwner(table, ownerId))
                    .as("zero orphans in %s", table).isZero();
        }
    }

    @Test
    void defaultValuesKeepMoneyConsistentWhenOmitted() {
        UUID ownerId = newOwner();
        UUID profileId = UUID.randomUUID();
        jdbc.update("insert into profile (id, owner_id) values (?, ?)", profileId, ownerId);

        var row = jdbc.queryForMap(
                "select currency, savings_amount, emergency_fund_amount, dependents_count from profile where id = ?",
                profileId);

        assertThat(row.get("currency")).isEqualTo("VND");
        assertThat((BigDecimal) row.get("savings_amount")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((BigDecimal) row.get("emergency_fund_amount")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(row.get("dependents_count")).isEqualTo(0);
    }
}
