package com.financialgps.application.debt.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import java.util.UUID;

/** Manually mark or undo this period's payment; no funds are moved and no balance is changed. */
public interface MarkDebtPayment {

    DebtModels.DebtView markPaid(OwnerId owner, UUID id);

    DebtModels.DebtView undoMark(OwnerId owner, UUID id);
}
