package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.domain.model.EffectiveDating;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.application.profile.port.out.IncomeStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * DTI denominator adapter: sums the owner's active income lines effective on the
 * requested asOfDate (never silently "today").
 * Empty when there is no income — the domain reports DTI UNAVAILABLE.
 *
 * <p>Uses the canonical {@link EffectiveDating} rule shared with CashFlowCalculator, so the
 * denominator can never disagree with the Financial Position on future-dated lines.
 */
@Component
class DebtIncomeReaderAdapter implements DebtIncomeReader {

    private final IncomeStore incomes;

    DebtIncomeReaderAdapter(IncomeStore incomes) {
        this.incomes = incomes;
    }

    @Override
    public Optional<BigDecimal> totalActiveMonthlyIncome(OwnerId owner, LocalDate asOfDate) {
        BigDecimal total = BigDecimal.ZERO;
        boolean any = false;
        for (var row : incomes.findAllByOwner(owner)) {
            if (EffectiveDating.isEffectiveOn(row.active(), row.effectiveFrom(), asOfDate)) {
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
