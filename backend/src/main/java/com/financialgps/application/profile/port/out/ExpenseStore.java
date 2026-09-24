package com.financialgps.application.profile.port.out;

import com.financialgps.application.account.model.OwnerId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Expense line persistence port. Owner-scoped by construction: no method can return another owner's
 * rows, and the adapter preserves the database-level owner predicate and FK cascade.
 */
public interface ExpenseStore {

    /** All of the owner's expense lines, oldest first (export ordering is applied by the use case). */
    List<ExpenseRecord> findAllByOwner(OwnerId owner);

    List<ExpenseRecord> findAllByProfile(UUID profileId, OwnerId owner);

    Optional<ExpenseRecord> findById(UUID id, OwnerId owner);

    /** {@code id == null} inserts, otherwise the owner-scoped row is updated. */
    ExpenseRecord save(ExpenseRecord expense);

    /** @return {@code true} when an owner-scoped row was removed. */
    boolean delete(UUID id, OwnerId owner);
}
