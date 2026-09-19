package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.domain.model.Money;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 001 TDD RED (Slice 1 / P2): owner-scoped repositories — precision round-trip and the
 * {@code findBy...AndOwnerId} convention that makes a cross-owner leak impossible by construction.
 */
@Transactional
class ProfileRepositoryTest extends IntegrationTestBase {

    @Autowired
    private ProfileRepository profiles;

    @Autowired
    private IncomeRepository incomes;

    @Autowired
    private ExpenseRepository expenses;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private ProfileEntity saveProfile(UUID ownerId) {
        return profiles.saveAndFlush(new ProfileEntity(ownerId, "VND",
                new BigDecimal("19.99"), new BigDecimal("0.01"), 2));
    }

    /** The profile.owner_id FK is real: repository fixtures need an account row. */
    private UUID newOwner() {
        UUID ownerId = UUID.randomUUID();
        jdbc.update("insert into account (id, email, password_hash, role) values (?, ?, ?, ?)",
                ownerId, "repo-" + ownerId + "@example.com", "$2a$12$fixturehash", "OWNER");
        return ownerId;
    }

    @Test
    void numericMoneyRoundTripsWithoutPrecisionLoss() {
        UUID ownerId = newOwner();
        ProfileEntity profile = saveProfile(ownerId);
        incomes.saveAndFlush(new IncomeEntity(ownerId, profile.getId(), new BigDecimal("19.99"),
                "salary", true, LocalDate.now()));
        incomes.saveAndFlush(new IncomeEntity(ownerId, profile.getId(), new BigDecimal("0.01"),
                "side", true, LocalDate.now()));

        ProfileEntity reloaded = profiles.findById(profile.getId()).orElseThrow();
        assertThat(reloaded.getSavingsAmount().toPlainString()).isEqualTo("19.99");
        assertThat(reloaded.getEmergencyFundAmount().toPlainString()).isEqualTo("0.01");

        List<IncomeEntity> rows = incomes.findByProfileIdAndOwnerId(profile.getId(), ownerId);
        Money sum = Money.zero("VND");
        for (IncomeEntity row : rows) {
            // The row's decimal string is the only thing that crosses into the domain (no double).
            sum = sum.add(Money.of(row.getAmount().toPlainString(), "VND"));
        }
        assertThat(sum.asDecimalString()).isEqualTo("20.00");
    }

    @Test
    void ownerScopedLookupsCannotSeeAnotherOwnersRows() {
        UUID ownerA = newOwner();
        UUID ownerB = newOwner();
        ProfileEntity profileA = saveProfile(ownerA);
        ProfileEntity profileB = saveProfile(ownerB);
        IncomeEntity incomeA = incomes.saveAndFlush(new IncomeEntity(ownerA, profileA.getId(),
                new BigDecimal("74.00"), "salary", true, LocalDate.now()));
        ExpenseEntity expenseB = expenses.saveAndFlush(new ExpenseEntity(ownerB, profileB.getId(),
                new BigDecimal("30.00"), "rent", "FIXED", true, LocalDate.now()));

        assertThat(profiles.findByOwnerId(ownerA)).get().extracting(ProfileEntity::getId)
                .isEqualTo(profileA.getId());
        assertThat(profiles.findByIdAndOwnerId(profileB.getId(), ownerA)).isEmpty();
        assertThat(incomes.findByIdAndOwnerId(incomeA.getId(), ownerB)).isEmpty();
        assertThat(incomes.findByProfileIdAndOwnerId(profileA.getId(), ownerA)).hasSize(1);
        assertThat(incomes.findByProfileIdAndOwnerId(profileA.getId(), ownerB)).isEmpty();
        assertThat(expenses.findByIdAndOwnerId(expenseB.getId(), ownerA)).isEmpty();
        assertThat(expenses.findByProfileIdAndOwnerId(profileB.getId(), ownerA)).isEmpty();
        assertThat(expenses.findByOwnerIdOrderByCreatedAt(ownerA)).isEmpty();
    }

    @Test
    void ownerScopedDeleteOnlyAffectsTheOwnersRows() {
        UUID ownerA = newOwner();
        UUID ownerB = newOwner();
        ProfileEntity profileA = saveProfile(ownerA);
        ProfileEntity profileB = saveProfile(ownerB);
        IncomeEntity incomeA = incomes.saveAndFlush(new IncomeEntity(ownerA, profileA.getId(),
                new BigDecimal("74.00"), "salary", true, LocalDate.now()));

        assertThat(incomes.deleteByIdAndOwnerId(incomeA.getId(), ownerB)).isZero();
        assertThat(rowCount("income", incomeA.getId())).isEqualTo(1);
        assertThat(incomes.deleteByIdAndOwnerId(incomeA.getId(), ownerA)).isEqualTo(1);
        assertThat(rowCount("income", incomeA.getId())).isZero();

        assertThat(profiles.deleteByIdAndOwnerId(profileA.getId(), ownerB)).isZero();
        assertThat(rowCount("profile", profileA.getId())).isEqualTo(1);
        assertThat(profiles.deleteByIdAndOwnerId(profileA.getId(), ownerA)).isEqualTo(1);
        assertThat(rowCount("profile", profileA.getId())).isZero();
        assertThat(profiles.findByIdAndOwnerId(profileB.getId(), ownerB)).isPresent();
    }

    /** Raw count: a bulk JPQL delete bypasses the persistence context, so counts are the truth. */
    private long rowCount(String table, UUID id) {
        Long count = jdbc.queryForObject("select count(*) from \"" + table + "\" where id = ?", Long.class, id);
        return count == null ? 0L : count;
    }
}
