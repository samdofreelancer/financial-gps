package com.financialgps.infrastructure.security;

import com.financialgps.application.account.port.out.CurrentCaller;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.platform.security.CurrentOwnerProvider;
import org.springframework.stereotype.Component;

/**
 * Adapter for {@link CurrentCaller}: delegates to the platform security context reader.
 * Infrastructure already depends on the platform lane (security configuration lives there);
 * the application and API lanes depend only on the port.
 */
@Component
class CallerContextAdapter implements CurrentCaller {

    private final CurrentOwnerProvider owners;

    CallerContextAdapter(CurrentOwnerProvider owners) {
        this.owners = owners;
    }

    @Override
    public OwnerId requireCurrentOwner() {
        return owners.requireCurrentOwner();
    }
}
