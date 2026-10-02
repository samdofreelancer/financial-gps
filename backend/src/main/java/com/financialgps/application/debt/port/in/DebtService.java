package com.financialgps.application.debt.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import java.util.List;
import java.util.UUID;

/** Use case: create / read / update / archive debts and read the portfolio summary. */
public interface DebtService {

    DebtModels.DebtView record(OwnerId owner, DebtModels.DebtCommand command);

    DebtModels.DebtView update(OwnerId owner, UUID id, DebtModels.DebtUpdateCommand command);

    void delete(OwnerId owner, UUID id);

    DebtModels.DebtView get(OwnerId owner, UUID id);

    List<DebtModels.DebtView> list(OwnerId owner);

    DebtModels.DebtSummaryView summary(OwnerId owner);
}
