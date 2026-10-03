package com.financialgps.infrastructure.time;

import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.profile.port.out.BusinessDate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * Debt-context adapter for {@link DebtBusinessDate}.
 *
 * <p>It delegates to the single clock adapter ({@link BusinessDate}) instead of reading a wall clock
 * again, so the "one production clock" rule still holds while the debt context keeps its own port
 * (plan §2.2).
 */
@Component
class SystemDebtBusinessDate implements DebtBusinessDate {

    private final BusinessDate clock;

    SystemDebtBusinessDate(BusinessDate clock) {
        this.clock = clock;
    }

    @Override
    public LocalDate today() {
        return clock.today();
    }
}