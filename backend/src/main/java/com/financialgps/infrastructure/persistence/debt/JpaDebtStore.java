package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.port.out.DebtRecord;
import com.financialgps.application.debt.port.out.DebtStore;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter for {@link DebtStore}: immutable records cross the port, never entities. */
@Component
class JpaDebtStore implements DebtStore {

    private final DebtRepository debts;

    JpaDebtStore(DebtRepository debts) {
        this.debts = debts;
    }

    @Override
    public List<DebtRecord> findAllByOwner(OwnerId owner) {
        return debts.findByOwnerIdAndStatusInOrderByCreatedAt(owner.value(), List.of("ACTIVE", "PAID_OFF"))
                .stream().map(JpaDebtStore::toRecord).toList();
    }

    @Override
    public List<DebtRecord> findAllActiveAndPaidByOwner(OwnerId owner) {
        return findAllByOwner(owner);
    }

    @Override
    public Optional<DebtRecord> findByIdAndOwner(UUID id, OwnerId owner) {
        return debts.findByIdAndOwnerId(id, owner.value())
                .filter(e -> !"ARCHIVED".equals(e.getStatus()))
                .map(JpaDebtStore::toRecord);
    }

    @Override
    public DebtRecord save(DebtRecord debt) {
        DebtEntity entity;
        if (debt.id() == null) {
            entity = new DebtEntity(debt.owner().value(), debt.creditor(), debt.debtType(),
                    amountOrNull(debt.originalPrincipal()), amount(debt.outstandingBalance()),
                    rate(debt.annualInterestRate()), amount(debt.minimumPayment()),
                    amount(debt.plannedPayment()), debt.dueDay(), debt.status());
        } else {
            entity = debts.findByIdAndOwnerId(debt.id(), debt.owner().value())
                    .filter(e -> !"ARCHIVED".equals(e.getStatus()))
                    .orElseThrow(() -> new com.financialgps.application.account.ResourceNotFoundException());
            entity.setCreditor(debt.creditor());
            entity.setDebtType(debt.debtType());
            entity.setOriginalPrincipal(amountOrNull(debt.originalPrincipal()));
            entity.setOutstandingBalance(amount(debt.outstandingBalance()));
            entity.setAnnualInterestRate(rate(debt.annualInterestRate()));
            entity.setMinimumPayment(amount(debt.minimumPayment()));
            entity.setPlannedPayment(amount(debt.plannedPayment()));
            entity.setDueDay(debt.dueDay());
            entity.setStatus(debt.status());
            entity.touch();
        }
        return toRecord(debts.save(entity));
    }

    @Override
    public DebtRecord setPaymentMarkedOn(UUID id, OwnerId owner, java.time.LocalDate markedOn) {
        DebtEntity entity = debts.findByIdAndOwnerId(id, owner.value())
                .filter(e -> !"ARCHIVED".equals(e.getStatus()))
                .orElseThrow(com.financialgps.application.account.ResourceNotFoundException::new);
        entity.setPaymentMarkedOn(markedOn);
        entity.touch();
        return toRecord(debts.save(entity));
    }

    @Override
    public boolean archiveByIdAndOwner(UUID id, OwnerId owner) {
        return debts.archiveByIdAndOwnerId(id, owner.value()) > 0;
    }

    private static BigDecimal amount(String decimal) {
        return new BigDecimal(decimal);
    }

    /** Nullable money: {@code null} means unknown and is stored as NULL, never as 0.00. */
    private static BigDecimal amountOrNull(String decimal) {
        return decimal == null ? null : new BigDecimal(decimal);
    }

    private static BigDecimal rate(String decimal) {
        return decimal == null ? null : new BigDecimal(decimal);
    }

    private static DebtRecord toRecord(DebtEntity entity) {
        return new DebtRecord(entity.getId(), new OwnerId(entity.getOwnerId()), entity.getCreditor(),
                entity.getDebtType(),
                entity.getOriginalPrincipal() == null ? null : entity.getOriginalPrincipal().toPlainString(),
                entity.getOutstandingBalance().toPlainString(),
                entity.getAnnualInterestRate() == null ? null : entity.getAnnualInterestRate().toPlainString(),
                entity.getMinimumPayment().toPlainString(), entity.getPlannedPayment().toPlainString(),
                entity.getDueDay(), entity.getStatus(), entity.getPaymentMarkedOn());
    }
}
