package com.financialgps.application.account.port.out;

import com.financialgps.application.account.model.OwnerId;

import java.util.Optional;

/**
 * Owner/account persistence port (Identity &amp; Access).
 *
 * <p>All email matching is case-insensitive (FR-001/FR-004, research §5). JPA entities and Spring
 * Data repositories are implementation details of the adapter, never part of this contract.
 */
public interface AccountStore {

    /** The account row of {@code owner}, or empty when it no longer exists. */
    Optional<AccountRecord> findById(OwnerId owner);

    /** The account holding {@code email}, matched case-insensitively, or empty. */
    Optional<AccountRecord> findByEmail(String email);

    /**
     * Case-insensitive existence probe. A courtesy fast path only — it avoids hashing for an
     * obvious duplicate and is explicitly <em>not</em> the uniqueness authority.
     */
    boolean emailExists(String email);

    /**
     * Inserts a new account and reaches the database before returning, so a lost race on the
     * case-insensitive unique email index surfaces here as {@link DuplicateAccountException}
     * instead of failing later at commit time where no contract could catch it.
     */
    AccountRecord insert(AccountRecord account);

    /** Hard delete; every owned row follows through the FK cascade (FR-012, SC-007). */
    void delete(OwnerId owner);
}
