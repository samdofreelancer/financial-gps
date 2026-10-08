package com.financialgps.application.debt.usecase;

import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.model.DomainValidationException;

/**
 * Shared user-input → domain mapping for debt writes. Currency is position-derived (spec 002
 * §7) and passed in — this mapper never hardcodes one.
 */
final class DebtCommandMapper {

    private DebtCommandMapper() {
    }

    static Debt toDomain(String currency, String creditor, String debtType, String originalPrincipal,
                         String outstandingBalance, String annualInterestRate,
                         String minimumPayment, String plannedPayment, Integer dueDay) {
        try {
            return Debt.recorded(currency, creditor, debtType, originalPrincipal,
                    outstandingBalance, annualInterestRate, minimumPayment, plannedPayment, dueDay);
        } catch (DomainValidationException e) {
            throw new DebtValidationException(e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DebtValidationException("DEBT_INVALID_TYPE", "Unknown debt type: " + debtType);
        }
    }
}
