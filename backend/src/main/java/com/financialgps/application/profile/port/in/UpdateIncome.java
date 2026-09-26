package com.financialgps.application.profile.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;

import java.util.UUID;

/**
 * Use case: replace an income line's editable fields (PUT /incomes/{id}). A cross-owner id is
 * indistinguishable from a missing one (FR-010).
 */
public interface UpdateIncome {

    ProfileModels.IncomeView update(OwnerId owner, UUID id, ProfileModels.IncomeCommand command);
}
