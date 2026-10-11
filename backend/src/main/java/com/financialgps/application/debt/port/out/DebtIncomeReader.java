package com.financialgps.application.debt.port.out;

import com.financialgps.domain.model.OwnerId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Reads the DTI denominator: total active monthly income effective on asOfDate.
 * Empty when the owner has no income (→ DTI UNAVAILABLE, never a divide-by-zero).
 */
public interface DebtIncomeReader {

    Optional<BigDecimal> totalActiveMonthlyIncome(OwnerId owner, LocalDate asOfDate);
}
