package com.financialgps.domain.debt;

import com.financialgps.domain.model.OwnerId;

import java.util.List;
import java.util.Optional;

/**
 * Debt repository port (plan §2.2): owner-scoped by construction, operating on domain
 * {@link Debt} aggregates only. Soft-delete only — the adapter transitions status to ARCHIVED and
 * never hard-deletes (hard delete is the account CASCADE).
 *
 * <p>Amounts inside {@link Debt} are self-describing on currency (single-currency MVP), so no
 * currency parameter is needed here.
 */
public interface DebtStore {

    /** All non-archived debts of the owner (ACTIVE + PAID_OFF), never ARCHIVED. */
    List<Debt> findAllByOwner(OwnerId owner);

    Optional<Debt> findByIdAndOwner(DebtId id, OwnerId owner);

    /** Create or update; returns the stored aggregate with its id assigned. */
    Debt save(OwnerId owner, Debt debt);

    boolean archiveByIdAndOwner(DebtId id, OwnerId owner);
}
