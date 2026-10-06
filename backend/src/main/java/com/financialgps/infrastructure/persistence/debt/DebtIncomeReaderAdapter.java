package com.financialgps.infrastructure.persistence.debt;

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
 * <p>Mirrors the CashFlowCalculator rule ({@code active && effectiveFrom <= asOf}): a future-dated
 * line must not inflate the denominator, or DTI would disagree with the Financial Position.
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
            if (row.active() && !row.effectiveFrom().isAfter(asOf)) {
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
