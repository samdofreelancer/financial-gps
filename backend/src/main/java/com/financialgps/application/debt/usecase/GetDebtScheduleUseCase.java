package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.GetDebtSchedule;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

/** Payment calendar of one debt, expressed in the position currency. */
public final class GetDebtScheduleUseCase implements GetDebtSchedule {

    private final DebtStore debts;
    private final DebtCurrency currency;
    private final DebtBusinessDate dates;

    public GetDebtScheduleUseCase(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates) {
        this.debts = debts;
        this.currency = currency;
        this.dates = dates;
    }

    @Override
    public DebtModels.DebtScheduleView schedule(OwnerId owner, UUID id) {
        Debt debt = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        return DebtViews.scheduleView(debt.withCurrency(currency.currencyOf(owner)), dates.today());
    }
}
