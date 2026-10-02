package com.financialgps.application.debt.port.out;

import com.financialgps.application.account.model.OwnerId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Debt persistence port (plan §2.2). Owner-scoped by construction; soft-delete only — the adapter
 * transitions status to ARCHIVED and never hard-deletes (hard delete is the account CASCADE).
 */
public interface DebtStore {

    List<DebtRecord> findAllByOwner(OwnerId owner);

    List<DebtRecord> findAllActiveAndPaidByOwner(OwnerId owner);

    Optional<DebtRecord> findByIdAndOwner(UUID id, OwnerId owner);

    DebtRecord save(DebtRecord debt);

    boolean archiveByIdAndOwner(UUID id, OwnerId owner);
}
