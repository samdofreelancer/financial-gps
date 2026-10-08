package com.financialgps.application.account.port.out;

import com.financialgps.domain.model.OwnerId;

/**
 * Outbound port: who is calling. HTTP adapters resolve the actor through this port instead of
 * touching session/principal machinery — the only implementation reads the platform security
 * context, so "who am I?" has exactly one answer everywhere.
 */
public interface CurrentCaller {

    /** @return the owner id of the current session, or throws {@code AuthRequiredException}. */
    OwnerId requireCurrentOwner();
}
