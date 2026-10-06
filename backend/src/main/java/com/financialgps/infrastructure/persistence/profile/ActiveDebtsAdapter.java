package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.port.out.DebtRecord;
import com.financialgps.application.debt.port.out.DebtStore;
import com.financialgps.application.profile.port.out.ActiveDebts;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStatus;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for {@link ActiveDebts}: the 002 → 001 Financial Position bridge.
 *
 * <p>Reads the owner's non-archived debt rows through the debt {@link DebtStore} and reconstitutes
 * pure domain {@link Debt}s via the domain factory, so the mandatory minimum payments reach
 * {@code CashFlowCalculator}. Only debts that contribute to totals are returned (ACTIVE); archived
 * and paid-off rows never reduce the owner's available capacity.
 */
@Component
class ActiveDebtsAdapter implements ActiveDebts {

    private final DebtStore debts;

    ActiveDebtsAdapter(DebtStore debts) {
        this.debts = debts;
    }

    @Override
    public List<Debt> findAllActive(OwnerId owner, String currency) {
        List<Debt> active = new ArrayList<>();
        for (DebtRecord record : debts.findAllByOwner(owner)) {
            Debt debt = Debt.reconstitute(currency, record.creditor(), record.debtType(),
                    record.originalPrincipal(), record.outstandingBalance(),
                    record.annualInterestRate(), record.minimumPayment(), record.plannedPayment(),
                    record.dueDay(), DebtStatus.valueOf(record.status()));
            if (debt.contributesToTotals()) {
                active.add(debt);
            }
        }
        return List.copyOf(active);
    }
}
