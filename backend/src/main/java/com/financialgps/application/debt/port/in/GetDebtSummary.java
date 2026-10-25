package com.financialgps.application.debt.port.in;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;

import java.time.LocalDate;

/** Use case: portfolio summary, DTI and payoff projection (GET /debts/summary). */
public interface GetDebtSummary {

    DebtModels.DebtSummaryView summary(OwnerId owner);

    /**
     * Deterministic variant evaluated as of an explicit date. The default delegates to the
     * business-date variant so existing callers keep their behaviour.
     */
    default DebtModels.DebtSummaryView summary(OwnerId owner, LocalDate asOfDate) {
        return summary(owner);
    }
}
