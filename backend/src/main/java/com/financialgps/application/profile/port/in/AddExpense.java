package com.financialgps.application.profile.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;

/** Use case: add an expense line (POST /expenses). Requires an existing profile. */
public interface AddExpense {

    ProfileModels.ExpenseView add(OwnerId owner, ProfileModels.ExpenseCommand command);
}
