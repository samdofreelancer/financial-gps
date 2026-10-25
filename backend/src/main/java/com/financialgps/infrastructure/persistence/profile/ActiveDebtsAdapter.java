package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.profile.port.out.ActiveDebts;
import com.financialgps.domain.debt.Debt;
import com.financialgps.application.debt.port.out.DebtStore;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for {@link ActiveDebts}: the 002 → 001 Financial Position bridge.
 *
 * <p>Only debts that contribute to totals are returned (ACTIVE); archived and paid-off rows never
 * reduce the owner's available capacity. The portfolio is expressed in the profile currency so the
 * mandatory payments reach {@code CashFlowCalculator} in the same currency as the cash flow they
 * reduce.
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
        for (Debt debt : debts.findAllByOwner(owner)) {
            if (debt.contributesToTotals()) {
                active.add(debt.withCurrency(currency));
            }
        }
        return List.copyOf(active);
    }
}
