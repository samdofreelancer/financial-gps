package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.application.profile.port.out.IncomeStore;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * DTI denominator adapter: sums the owner's active income lines.
 * Empty when there is no income — the domain reports DTI UNAVAILABLE.
 */
@Component
class DebtIncomeReaderAdapter implements DebtIncomeReader {

    private final IncomeStore incomes;

    DebtIncomeReaderAdapter(IncomeStore incomes) {
        this.incomes = incomes;
    }

    @Override
    public Optional<BigDecimal> totalActiveMonthlyIncome(OwnerId owner) {
        BigDecimal total = BigDecimal.ZERO;
        boolean any = false;
        for (var row : incomes.findAllByOwner(owner)) {
            if (row.active()) {
                total = total.add(new BigDecimal(row.amount()));
                any = true;
            }
        }
        if (!any || total.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        return Optional.of(total);
    }
}
