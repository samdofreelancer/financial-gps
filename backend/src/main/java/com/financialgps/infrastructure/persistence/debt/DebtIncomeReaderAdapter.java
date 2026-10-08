package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.domain.model.Income;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.IncomeStore;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * DTI denominator adapter: sums the owner's active income lines effective on the business date.
 * Empty when there is no income — the domain reports DTI UNAVAILABLE.
 *
 * <p>The effective-date rule is domain-owned ({@link Income#isEffective}): this adapter applies
 * the same predicate {@code CashFlowCalculator} uses instead of reimplementing it, so the
 * denominator can never disagree with the Financial Position. Currency is the stored profile
 * currency (debts inherit it, spec 002 §7).
 */
@Component
class DebtIncomeReaderAdapter implements DebtIncomeReader {

    private final IncomeStore incomes;
    private final BusinessDate businessDate;

    DebtIncomeReaderAdapter(IncomeStore incomes, BusinessDate businessDate) {
        this.incomes = incomes;
        this.businessDate = businessDate;
    }

    @Override
    public Optional<BigDecimal> totalActiveMonthlyIncome(OwnerId owner) {
        var asOf = businessDate.today();
        BigDecimal total = BigDecimal.ZERO;
        boolean any = false;
        for (var row : incomes.findAllByOwner(owner)) {
            if (Income.isEffective(row.active(), row.effectiveFrom(), asOf)) {
                // Canonical scale guarded by the numeric(19,2) column; no Money construction
                // outside the domain lane (architecture guard).
                total = total.add(new BigDecimal(row.amount()).setScale(Money.SCALE));
                any = true;
            }
        }
        if (!any || total.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        return Optional.of(total);
    }
}
