package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** T006 RED: owner-scoped debt repositories + archive (soft-delete) semantics. */
@Transactional
class DebtRepositoryTest extends IntegrationTestBase {

    @Autowired
    private EntityManager entities;

    @Autowired
    private DebtRepository debts;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID newOwner() {
        UUID ownerId = UUID.randomUUID();
        jdbc.update("insert into account (id, email, password_hash, role) values (?, ?, ?, ?)",
                ownerId, "debt-repo-" + ownerId + "@example.com", "$2a$12$fixturehash", "OWNER");
        return ownerId;
    }

    private DebtEntity save(UUID ownerId, String status) {
        return debts.saveAndFlush(new DebtEntity(ownerId, "Bank", "CREDIT_CARD",
                new BigDecimal("20000000.00"), new BigDecimal("15000000.00"),
                new BigDecimal("0.180000"), new BigDecimal("1500000.00"),
                new BigDecimal("3000000.00"), 15, status));
    }

    @Test
    void activeAndPaidAreListed_archivedIsExcluded() {
        UUID ownerId = newOwner();
        DebtEntity active = save(ownerId, "ACTIVE");
        save(ownerId, "PAID_OFF");
        save(ownerId, "ARCHIVED");

        List<DebtEntity> listed =
                debts.findByOwnerIdAndStatusInOrderByCreatedAt(ownerId, List.of("ACTIVE", "PAID_OFF"));
        assertThat(listed).extracting(DebtEntity::getId)
                .contains(active.getId());
        assertThat(listed).hasSize(2);
        assertThat(listed).extracting(DebtEntity::getStatus)
                .containsExactlyInAnyOrder("ACTIVE", "PAID_OFF");
        assertThat(active.getOutstandingBalance().toPlainString()).isEqualTo("15000000.00");
    }

    @Test
    void ownerScopedLookupsCannotSeeAnotherOwnersRows() {
        UUID ownerA = newOwner();
        UUID ownerB = newOwner();
        DebtEntity debtA = save(ownerA, "ACTIVE");

        assertThat(debts.findByIdAndOwnerId(debtA.getId(), ownerB)).isEmpty();
        assertThat(debts.findByOwnerIdOrderByCreatedAt(ownerB)).isEmpty();
        assertThat(debts.findByIdAndOwnerId(debtA.getId(), ownerA)).isPresent();
    }

    @Test
    void archiveTransitionsToArchivedAndIsIdempotent() {
        UUID ownerA = newOwner();
        UUID ownerB = newOwner();
        DebtEntity debt = save(ownerA, "ACTIVE");
        UUID debtId = debt.getId();

        assertThat(debts.archiveByIdAndOwnerId(debtId, ownerB)).isZero();
        assertThat(debts.archiveByIdAndOwnerId(debtId, ownerA)).isEqualTo(1);
        entities.flush();
        entities.clear();

        assertThat(debts.findByIdAndOwnerId(debtId, ownerA).orElseThrow().getStatus())
                .isEqualTo("ARCHIVED");
        assertThat(debts.archiveByIdAndOwnerId(debtId, ownerA)).isZero();
        assertThat(debts.findByOwnerIdAndStatusInOrderByCreatedAt(ownerA, List.of("ACTIVE", "PAID_OFF")))
                .isEmpty();
    }
}
