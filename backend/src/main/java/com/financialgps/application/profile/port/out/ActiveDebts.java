package com.financialgps.application.profile.port.out;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.domain.debt.Debt;
import java.util.List;

/**
 * Outbound port: the owner's ACTIVE debts as pure domain aggregates, ready to be handed to
 * {@code CashFlowCalculator} as the 002 {@code Mandatory Payment} input (spec §8.2).
 *
 * <p>Only debts that contribute to totals are returned (ACTIVE, spec §4.3): archived and paid-off
 * rows are excluded here so the profile position never charges the owner for a settled debt. The
 * port returns domain objects, not persistence records — the profile context never learns the debt
 * context's storage shape.
 */
public interface ActiveDebts {

    /**
     * @param owner    the authenticated owner whose debts are read
     * @param currency the owner's single-currency debt currency (debts inherit the profile currency;
     *                 VND when no profile exists — spec §7/§10.3)
     * @return the owner's ACTIVE debts, never {@code null}
     */
    List<Debt> findAllActive(OwnerId owner, String currency);
}