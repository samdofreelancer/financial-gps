package com.financialgps.application.profile.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;

import java.util.UUID;

/**
 * Use case: replace an expense line's editable fields (PUT /expenses/{id}). A cross-owner id is
 * indistinguishable from a missing one (FR-010).
 */
public interface UpdateExpense {

    ProfileModels.ExpenseView update(OwnerId owner, UUID id, ProfileModels.ExpenseCommand command);
}
