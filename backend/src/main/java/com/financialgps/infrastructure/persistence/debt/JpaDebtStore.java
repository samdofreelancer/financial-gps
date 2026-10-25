package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.application.debt.port.out.DebtStore;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter for {@link DebtStore}: domain aggregates cross the port, never entities. */
@Component
class JpaDebtStore implements DebtStore {

    private final DebtRepository debts;

    JpaDebtStore(DebtRepository debts) {
        this.debts = debts;
    }

    @Override
    public List<Debt> findAllByOwner(OwnerId owner) {
        return debts.findByOwnerIdAndStatusInOrderByCreatedAt(owner.value(), List.of("ACTIVE", "PAID_OFF"))
                .stream().map(JpaDebtStore::toDomain).toList();
    }

    @Override
    public Optional<Debt> findByIdAndOwner(DebtId id, OwnerId owner) {
        return debts.findByIdAndOwnerId(id.value(), owner.value())
                .filter(e -> !"ARCHIVED".equals(e.getStatus()))
                .map(JpaDebtStore::toDomain);
    }

    @Override
    public Debt save(OwnerId owner, Debt debt) {
        DebtEntity entity;
        if (debt.id() == null) {
            entity = new DebtEntity(owner.value(), debt.creditor(), debt.debtType().name(),
                    amountOrNull(debt.originalPrincipal()), amount(debt.outstandingBalance()),
                    rate(debt.annualInterestRate()), amount(debt.minimumPayment()),
                    amount(debt.plannedPayment()), debt.dueDay(), debt.status().name());
            entity.setPaymentMarkedOn(debt.paymentMarkedOn());
        } else {
            entity = debts.findByIdAndOwnerId(debt.id().value(), owner.value())
                    .filter(e -> !"ARCHIVED".equals(e.getStatus()))
                    .orElseThrow(() -> new IllegalStateException(
                            "Debt " + debt.id().value() + " vanished between read and write"));
            entity.setCreditor(debt.creditor());
            entity.setDebtType(debt.debtType().name());
            entity.setOriginalPrincipal(amountOrNull(debt.originalPrincipal()));
            entity.setOutstandingBalance(amount(debt.outstandingBalance()));
            entity.setAnnualInterestRate(rate(debt.annualInterestRate()));
            entity.setMinimumPayment(amount(debt.minimumPayment()));
            entity.setPlannedPayment(amount(debt.plannedPayment()));
            entity.setDueDay(debt.dueDay());
            entity.setStatus(debt.status().name());
            entity.setPaymentMarkedOn(debt.paymentMarkedOn());
            entity.touch();
        }
        return toDomain(debts.save(entity));
    }

    @Override
    public boolean archiveByIdAndOwner(DebtId id, OwnerId owner) {
        return debts.archiveByIdAndOwnerId(id.value(), owner.value()) > 0;
    }

    private static BigDecimal amount(com.financialgps.domain.model.Money money) {
        return new BigDecimal(money.asDecimalString());
    }

    /** Nullable money: {@code null} means unknown and is stored as NULL, never as 0.00. */
    private static BigDecimal amountOrNull(com.financialgps.domain.model.Money money) {
        return money == null ? null : new BigDecimal(money.asDecimalString());
    }

    private static BigDecimal rate(com.financialgps.domain.debt.Rate rate) {
        return rate == null ? null : new BigDecimal(rate.asDecimalString());
    }

    private static Debt toDomain(DebtEntity entity) {
        return Debt.reconstitute(DebtId.of(entity.getId()), Debt.DEFAULT_CURRENCY, entity.getCreditor(),
                entity.getDebtType(),
                entity.getOriginalPrincipal() == null ? null : entity.getOriginalPrincipal().toPlainString(),
                entity.getOutstandingBalance().toPlainString(),
                entity.getAnnualInterestRate() == null ? null : entity.getAnnualInterestRate().toPlainString(),
                entity.getMinimumPayment().toPlainString(), entity.getPlannedPayment().toPlainString(),
                entity.getDueDay(), com.financialgps.domain.debt.DebtStatus.valueOf(entity.getStatus()),
                entity.getPaymentMarkedOn());
    }
}
