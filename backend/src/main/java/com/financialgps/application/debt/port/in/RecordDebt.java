package com.financialgps.application.debt.port.in;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import java.util.UUID;

/** Use case: create a debt (POST /debts → 201). */
public interface RecordDebt {

    DebtModels.DebtView record(OwnerId owner, DebtModels.DebtCommand command);
}
