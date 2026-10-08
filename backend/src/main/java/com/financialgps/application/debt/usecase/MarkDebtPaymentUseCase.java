package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.MarkDebtPayment;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.domain.debt.DebtStatus;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.OwnerId;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Manual current-period payment marker (no funds move; null clears it). Only ACTIVE debts can
 * be marked.
 */
public final class MarkDebtPaymentUseCase implements MarkDebtPayment {

    private final DebtStore debts;
    private final DebtCurrency currency;
    private final DebtBusinessDate dates;

    public MarkDebtPaymentUseCase(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates) {
        this.debts = debts;
        this.currency = currency;
        this.dates = dates;
    }

    @Override
    public DebtModels.DebtView markPaid(OwnerId owner, UUID id) {
        return setPaymentMark(owner, id, dates.today());
    }

    @Override
    public DebtModels.DebtView undoMark(OwnerId owner, UUID id) {
        return setPaymentMark(owner, id, null);
    }

    private DebtModels.DebtView setPaymentMark(OwnerId owner, UUID id, LocalDate markedOn) {
        Debt current = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        if (current.status() != DebtStatus.ACTIVE) {
            throw new DebtValidationException("DEBT_NOT_ACTIVE", "Only active debts can be marked paid.");
        }
        String code = currency.currencyOf(owner);
        return DebtViews.view(
                debts.save(owner, current.withPaymentMarkedOn(markedOn)).withCurrency(code),
                dates.today());
    }
}
