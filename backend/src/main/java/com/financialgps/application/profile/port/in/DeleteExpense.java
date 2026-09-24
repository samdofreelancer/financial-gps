package com.financialgps.application.profile.port.in;

import com.financialgps.application.account.model.OwnerId;

import java.util.UUID;

/**
 * Use case: hard-delete an expense line (DELETE /expenses/{id}). A cross-owner id is
 * indistinguishable from a missing one (FR-010).
 */
public interface DeleteExpense {

    void delete(OwnerId owner, UUID id);
}
