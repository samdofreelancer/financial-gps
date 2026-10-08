package com.financialgps.application.debt.port.out;

import com.financialgps.domain.model.OwnerId;

/**
 * Outbound port: the currency new debt facts are recorded in. Debts inherit the owner's
 * Financial Position currency (spec 002 §7); the single implementation reads it from the
 * stored profile, defaulting exactly like the position reader when no profile exists.
 */
public interface DebtCurrency {

    /** @return the owner's position currency, never {@code null}. */
    String currencyOf(OwnerId owner);
}
