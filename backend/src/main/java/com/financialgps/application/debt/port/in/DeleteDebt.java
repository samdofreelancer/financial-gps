package com.financialgps.application.debt.port.in;

import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

/** Use case: soft-delete a debt to ARCHIVED (DELETE /debts/{id} → 204). */
public interface DeleteDebt {

    void delete(OwnerId owner, UUID id);
}
