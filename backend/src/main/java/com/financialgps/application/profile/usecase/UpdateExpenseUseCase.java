package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.UpdateExpense;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;

import java.util.UUID;

/**
 * Replace an expense line's editable fields. Only amount, category and type are editable: identity,
 * owner, profile, active flag and effective date are preserved. A cross-owner id yields the same
 * outcome as a missing one (FR-010).
 */
public final class UpdateExpenseUseCase implements UpdateExpense {

    private final ExpenseStore expenses;

    public UpdateExpenseUseCase(ExpenseStore expenses) {
        this.expenses = expenses;
    }

    @Override
    public ProfileModels.ExpenseView update(OwnerId owner, UUID id, ProfileModels.ExpenseCommand command) {
        ExpenseRecord current = expenses.findById(id, owner).orElseThrow(ResourceNotFoundException::new);
        ExpenseRecord saved = expenses.save(new ExpenseRecord(current.id(), owner, current.profileId(),
                command.amount(), command.category(), command.expenseType(),
                current.active(), current.effectiveFrom()));
        return new ProfileModels.ExpenseView(saved.id(), saved.amount(), saved.category(),
                saved.expenseType());
    }
}
