package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.UpdateDebt;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

/** Replace a debt fact, preserving identity and the payment mark. */
public final class UpdateDebtUseCase implements UpdateDebt {

    private final DebtStore debts;
    private final DebtCurrency currency;
    private final DebtBusinessDate dates;

    public UpdateDebtUseCase(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates) {
        this.debts = debts;
        this.currency = currency;
        this.dates = dates;
    }

    @Override
    public DebtModels.DebtView update(OwnerId owner, UUID id, DebtModels.DebtUpdateCommand command) {
        Debt existing = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        String code = currency.currencyOf(owner);
        Debt updated = DebtCommandMapper.toDomain(code, command.creditor(), command.debtType(),
                command.originalPrincipal(), command.outstandingBalance(),
                command.annualInterestRate(), command.minimumPayment(), command.plannedPayment(),
                command.dueDay())
                .withId(existing.id())
                .withPaymentMarkedOn(existing.paymentMarkedOn());
        return DebtViews.view(debts.save(owner, updated).withCurrency(code), dates.today());
    }
}
