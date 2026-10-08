package com.financialgps.application.debt.usecase;

import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.RecordDebt;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.OwnerId;

/** Record a new debt fact: lifecycle derived from the balance, currency from the position. */
public final class RecordDebtUseCase implements RecordDebt {

    private final DebtStore debts;
    private final DebtCurrency currency;
    private final DebtBusinessDate dates;

    public RecordDebtUseCase(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates) {
        this.debts = debts;
        this.currency = currency;
        this.dates = dates;
    }

    @Override
    public DebtModels.DebtView record(OwnerId owner, DebtModels.DebtCommand command) {
        String code = currency.currencyOf(owner);
        Debt saved = debts.save(owner, DebtCommandMapper.toDomain(code, command.creditor(),
                command.debtType(), command.originalPrincipal(), command.outstandingBalance(),
                command.annualInterestRate(), command.minimumPayment(), command.plannedPayment(),
                command.dueDay()));
        // The store reconstitutes with a currency-agnostic label: re-express before rendering.
        return DebtViews.view(saved.withCurrency(code), dates.today());
    }
}
