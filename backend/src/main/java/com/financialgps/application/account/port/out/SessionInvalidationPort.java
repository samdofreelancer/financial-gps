package com.financialgps.application.account.port.out;

import com.financialgps.domain.model.OwnerId;

/**
 * Session invalidation port (T1/T2/T3). Implemented in the platform security lane over the Spring
 * Session JDBC store — no servlet API, no HTTP, no database resource enters the account use cases.
 *
 * <p>{@link #invalidateAll} is the "kill every session" hammer (logout-all, account delete).
 * {@link #invalidateOthers} keeps exactly one session alive (change-password: the current one),
 * so the caller is not signed out by its own security action.
 */
public interface SessionInvalidationPort {

    /** Deletes every session row of the owner, including the caller's. */
    void invalidateAll(OwnerId owner);

    /**
     * Deletes every session row of the owner EXCEPT {@code keepSessionId} (when non-null).
     * A null {@code keepSessionId} is equivalent to {@link #invalidateAll}.
     */
    void invalidateOthers(OwnerId owner, String keepSessionId);
}
