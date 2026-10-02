package com.financialgps.application.debt.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import java.util.List;
import java.util.UUID;

/** Use case: read one debt / list debts (GET /debts, GET /debts/{id}). */
public interface GetDebts {

    DebtModels.DebtView get(OwnerId owner, UUID id);

    List<DebtModels.DebtView> list(OwnerId owner);
}
