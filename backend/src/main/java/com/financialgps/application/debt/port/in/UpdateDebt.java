package com.financialgps.application.debt.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import java.util.UUID;

/** Use case: update a debt (PUT /debts/{id} → 200). */
public interface UpdateDebt {

    DebtModels.DebtView update(OwnerId owner, UUID id, DebtModels.DebtUpdateCommand command);
}
