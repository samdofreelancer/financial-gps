package com.financialgps.application.debt.port.in;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import java.util.UUID;

/** Use case: payment schedule of one debt — date, principal, interest, ending balance (GET /debts/{id}/schedule). */
public interface GetDebtSchedule {

    DebtModels.DebtScheduleView schedule(OwnerId owner, UUID id);
}
