package com.financialgps.platform.security;

import com.financialgps.application.account.OwnerId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated owner from the security context (plan §Ownership flow, A4). This is
 * the ONLY place where session/principal material is inspected; services below receive the
 * returned {@code OwnerId} value explicitly.
 */
@Component
public class CurrentOwnerProvider {

    /** @return the owner id of the current session. */
    public OwnerId requireCurrentOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OwnerPrincipal principal) {
            return new OwnerId(principal.id());
        }
        throw new AuthRequiredException();
    }
}
