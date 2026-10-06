package com.financialgps.platform.security;

import com.financialgps.application.account.port.out.SessionInvalidationPort;
import com.financialgps.domain.model.OwnerId;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Spring Session JDBC implementation of {@link SessionInvalidationPort}: the {@code SPRING_SESSION}
 * table is indexed by {@code PRINCIPAL_NAME} (the owner id written by {@link OwnerPrincipal}), so a
 * single delete removes every server-side session of that owner — no cookie from a stolen device
 * survives logout-all / password change / account deletion.
 */
@Component
class SessionInvalidationAdapter implements SessionInvalidationPort {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    SessionInvalidationAdapter(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void invalidateAll(OwnerId owner) {
        deleteAll(sessions.findByPrincipalName(owner.value().toString()).keySet());
    }

    @Override
    public void invalidateOthers(OwnerId owner, String keepSessionId) {
        java.util.Set<String> ids = new java.util.HashSet<>(
                sessions.findByPrincipalName(owner.value().toString()).keySet());
        ids.remove(keepSessionId);
        deleteAll(ids);
    }

    private void deleteAll(java.util.Set<String> sessionIds) {
        for (String sessionId : sessionIds) {
            sessions.deleteById(sessionId);
        }
    }
}
