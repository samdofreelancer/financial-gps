package com.financialgps.application.profile.port.in;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;

/** Use case: add an income line (POST /incomes). Requires an existing profile. */
public interface AddIncome {

    ProfileModels.IncomeView add(OwnerId owner, ProfileModels.IncomeCommand command);
}
