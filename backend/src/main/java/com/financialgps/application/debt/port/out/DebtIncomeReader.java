package com.financialgps.application.debt.port.out;

import com.financialgps.application.account.model.OwnerId;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Reads the DTI denominator: total active monthly income effective on asOf.
 * Empty when the owner has no income (→ DTI UNAVAILABLE, never a divide-by-zero).
 */
public interface DebtIncomeReader {

    Optional<BigDecimal> totalActiveMonthlyIncome(OwnerId owner);
}
