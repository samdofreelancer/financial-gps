package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.domain.model.Obligation;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.profile.port.out.ActiveDebts;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStore;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for {@link ActiveDebts}: the 002 → 001 Financial Position bridge.
 *
 * <p>Only debts that contribute to totals are mapped (ACTIVE); archived and paid-off rows
 * never reduce the owner's available capacity. This mapping is the single place where the
 * debt aggregate crosses contexts — downstream sees shared-kernel obligations only. Amounts
 * are expressed in the profile currency so the mandatory payments reach
 * {@code CashFlowCalculator} in the same currency as the cash flow they reduce.
 */
@Component
class ActiveDebtsAdapter implements ActiveDebts {

    private final DebtStore debts;

    ActiveDebtsAdapter(DebtStore debts) {
        this.debts = debts;
    }

    @Override
    public List<Obligation> findAllActive(OwnerId owner, String currency) {
        List<Obligation> active = new ArrayList<>();
        for (Debt debt : debts.findAllByOwner(owner)) {
            if (debt.contributesToTotals()) {
                active.add(new Obligation(debt.withCurrency(currency).minimumPayment()));
            }
        }
        return List.copyOf(active);
    }
}
