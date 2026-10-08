package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.GetDebts;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.OwnerId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Read one debt or list the owner's debts, expressed in the position currency. */
public final class GetDebtsUseCase implements GetDebts {

    private final DebtStore debts;
    private final DebtCurrency currency;
    private final DebtBusinessDate dates;

    public GetDebtsUseCase(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates) {
        this.debts = debts;
        this.currency = currency;
        this.dates = dates;
    }

    @Override
    public DebtModels.DebtView get(OwnerId owner, UUID id) {
        Debt debt = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        return DebtViews.view(debt.withCurrency(currency.currencyOf(owner)), dates.today());
    }

    @Override
    public List<DebtModels.DebtView> list(OwnerId owner) {
        LocalDate asOf = dates.today();
        String code = currency.currencyOf(owner);
        List<DebtModels.DebtView> views = new ArrayList<>();
        for (Debt debt : debts.findAllByOwner(owner)) {
            views.add(DebtViews.view(debt.withCurrency(code), asOf));
        }
        return List.copyOf(views);
    }
}
