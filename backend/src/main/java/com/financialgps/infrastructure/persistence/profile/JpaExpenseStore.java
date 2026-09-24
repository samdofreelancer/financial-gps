package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter for {@link ExpenseStore}. Every query keeps an explicit owner predicate — the
 * owner-scoped repository convention (fixture convention / FR-007) is preserved verbatim, so no
 * adapter method can leak another owner's rows.
 */
@Component
class JpaExpenseStore implements ExpenseStore {

    private final ExpenseRepository expenses;

    JpaExpenseStore(ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    @Override
    public List<ExpenseRecord> findAllByOwner(OwnerId owner) {
        return expenses.findByOwnerIdOrderByCreatedAt(owner.value()).stream()
                .map(JpaExpenseStore::toRecord)
                .toList();
    }

    @Override
    public List<ExpenseRecord> findAllByProfile(UUID profileId, OwnerId owner) {
        return expenses.findByProfileIdAndOwnerId(profileId, owner.value()).stream()
                .map(JpaExpenseStore::toRecord)
                .toList();
    }

    @Override
    public Optional<ExpenseRecord> findById(UUID id, OwnerId owner) {
        return expenses.findByIdAndOwnerId(id, owner.value()).map(JpaExpenseStore::toRecord);
    }

    @Override
    public ExpenseRecord save(ExpenseRecord expense) {
        ExpenseEntity entity;
        if (expense.id() == null) {
            entity = new ExpenseEntity(expense.owner().value(), expense.profileId(),
                    new BigDecimal(expense.amount()), expense.category(), expense.expenseType(),
                    expense.active(), expense.effectiveFrom());
        } else {
            entity = expenses.findByIdAndOwnerId(expense.id(), expense.owner().value())
                    .orElseThrow(() -> new IllegalStateException(
                            "Expense line " + expense.id() + " vanished between read and write"));
            entity.setAmount(new BigDecimal(expense.amount()));
            entity.setCategory(expense.category());
            entity.setExpenseType(expense.expenseType());
            entity.touch();
        }
        return toRecord(expenses.save(entity));
    }

    @Override
    public boolean delete(UUID id, OwnerId owner) {
        return expenses.deleteByIdAndOwnerId(id, owner.value()) > 0;
    }

    private static ExpenseRecord toRecord(ExpenseEntity entity) {
        return new ExpenseRecord(entity.getId(), new OwnerId(entity.getOwnerId()),
                entity.getProfileId(), entity.getAmount().toPlainString(), entity.getCategory(),
                entity.getExpenseType(), entity.isActive(), entity.getEffectiveFrom());
    }
}
