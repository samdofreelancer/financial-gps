package com.financialgps.application.profile.port.out;

import com.financialgps.domain.model.Obligation;
import com.financialgps.domain.model.OwnerId;
import java.util.List;

/**
 * Outbound port: the owner's mandatory monthly commitments as shared-kernel
 * {@link Obligation}s, ready to be handed to {@code CashFlowCalculator} as the 002
 * {@code Mandatory Payment} input (spec §8.2).
 *
 * <p>Only commitments that contribute to totals are returned (ACTIVE debts, spec §4.3):
 * archived and paid-off rows are excluded here so the profile position never charges the
 * owner for a settled debt. The port returns shared-kernel value objects, never the debt
 * context's aggregates — the profile context never learns the debt context's shape.
 */
public interface ActiveDebts {

    /**
     * @param owner    the authenticated owner whose commitments are read
     * @param currency the owner's single-currency position currency (debts inherit the profile
     *                 currency; VND when no profile exists — spec §7/§10.3)
     * @return the owner's active obligations, never {@code null}
     */
    List<Obligation> findAllActive(OwnerId owner, String currency);
}
