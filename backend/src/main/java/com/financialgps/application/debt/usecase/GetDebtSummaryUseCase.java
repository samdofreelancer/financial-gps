package com.financialgps.application.debt.usecase;

import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.OwnerId;
import java.util.List;

/** Portfolio totals, DTI and payoff projection over the owner's contributing debts. */
public final class GetDebtSummaryUseCase implements GetDebtSummary {

    private final DebtStore debts;
    private final DebtCurrency currency;
    private final DebtIncomeReader incomes;
    private final DebtBusinessDate dates;

    public GetDebtSummaryUseCase(DebtStore debts, DebtCurrency currency, DebtIncomeReader incomes,
                                 DebtBusinessDate dates) {
        this.debts = debts;
        this.currency = currency;
        this.incomes = incomes;
        this.dates = dates;
    }

    @Override
    public DebtModels.DebtSummaryView summary(OwnerId owner) {
        String code = currency.currencyOf(owner);
        List<Debt> active = debts.findAllByOwner(owner).stream()
                .filter(Debt::contributesToTotals)
                .map(debt -> debt.withCurrency(code))
                .toList();
        return DebtViews.summaryView(active, incomes.totalActiveMonthlyIncome(owner).orElse(null),
                dates.today());
    }
}
