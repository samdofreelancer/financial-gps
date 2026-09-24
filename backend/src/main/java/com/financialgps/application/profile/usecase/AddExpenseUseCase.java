package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.AddExpense;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;

/**
 * Add an expense line to the owner's profile. A line cannot exist without its profile, so a missing
 * profile is the same {@code 404 RESOURCE_NOT_FOUND} outcome as a missing line (FR-010).
 */
public final class AddExpenseUseCase implements AddExpense {

    private final ProfileStore profiles;
    private final ExpenseStore expenses;
    private final BusinessDate businessDate;

    public AddExpenseUseCase(ProfileStore profiles, ExpenseStore expenses, BusinessDate businessDate) {
        this.profiles = profiles;
        this.expenses = expenses;
        this.businessDate = businessDate;
    }

    @Override
    public ProfileModels.ExpenseView add(OwnerId owner, ProfileModels.ExpenseCommand command) {
        ProfileRecord profile = profiles.findByOwner(owner).orElseThrow(ResourceNotFoundException::new);
        ExpenseRecord saved = expenses.save(new ExpenseRecord(null, owner, profile.id(),
                command.amount(), command.category(), command.expenseType(), true, businessDate.today()));
        return new ProfileModels.ExpenseView(saved.id(), saved.amount(), saved.category(),
                saved.expenseType());
    }
}
