package com.financialgps.application.debt.port.in;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;

/** Use case: portfolio summary, DTI and payoff projection (GET /debts/summary). */
public interface GetDebtSummary {

    DebtModels.DebtSummaryView summary(OwnerId owner);
}
