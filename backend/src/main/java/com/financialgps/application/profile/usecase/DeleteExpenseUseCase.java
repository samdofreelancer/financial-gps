package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.port.in.DeleteExpense;
import com.financialgps.application.profile.port.out.ExpenseStore;

import java.util.UUID;

/**
 * Hard-delete an expense line. The delete is owner-scoped in one statement, so a cross-owner id and a
 * missing id produce the identical outcome without a read-then-write race (FR-010).
 */
public final class DeleteExpenseUseCase implements DeleteExpense {

    private final ExpenseStore expenses;

    public DeleteExpenseUseCase(ExpenseStore expenses) {
        this.expenses = expenses;
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        if (!expenses.delete(id, owner)) {
            throw new ResourceNotFoundException();
        }
    }
}
