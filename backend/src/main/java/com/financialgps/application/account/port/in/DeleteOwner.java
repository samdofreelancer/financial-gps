package com.financialgps.application.account.port.in;

import com.financialgps.application.account.model.OwnerId;

/**
 * Use case: confirmed, irreversible account deletion (FR-012, FR-014, SC-007).
 *
 * <p>Boundary: this use case owns the database delete only. HTTP session invalidation deliberately
 * stays outside it (owned by the API adapter after this returns) — a servlet session is not a
 * database resource and must never be enlisted in the database transaction.
 */
public interface DeleteOwner {

    /** @param confirmation must equal the exact confirmation token, else the attempt is rejected. */
    void delete(OwnerId owner, String confirmation);
}
