package com.financialgps.application.account.usecase;

import com.financialgps.application.account.ConfirmationRequiredException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.in.DeleteOwner;
import com.financialgps.application.account.port.out.AccountStore;

/**
 * Confirmed account deletion (FR-012, FR-014, SC-007): one hard delete of the {@code account} row
 * inside a single application transaction; every owned row goes with it through
 * {@code owner_id REFERENCES account(id) ON DELETE CASCADE} (research §7). No soft-delete state.
 *
 * <p>Framework-free: the transaction is applied at the input-port boundary by an infrastructure
 * decorator (plan Phase 2 step 4).
 */
public final class DeleteOwnerUseCase implements DeleteOwner {

    public static final String REQUIRED_CONFIRMATION = "DELETE";

    private final AccountStore accounts;

    public DeleteOwnerUseCase(AccountStore accounts) {
        this.accounts = accounts;
    }

    @Override
    public void delete(OwnerId owner, String confirmation) {
        if (!REQUIRED_CONFIRMATION.equals(confirmation)) {
            throw new ConfirmationRequiredException();
        }
        accounts.delete(owner);
    }
}
