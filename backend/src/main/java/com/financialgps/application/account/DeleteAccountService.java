package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirmed account deletion (FR-012, FR-014, SC-007): one hard delete of the {@code account} row
 * inside a single application transaction; every owned row goes with it through
 * {@code owner_id REFERENCES account(id) ON DELETE CASCADE} (research §7). No soft-delete state.
 *
 * <p>Boundary: this transaction covers ONLY the database delete. HTTP session invalidation
 * deliberately stays outside it (owned by the API layer via {@code SessionAuthenticator#logout}
 * after this method returns) — a servlet session is not a database resource and must never be
 * enlisted in the DB transaction.
 */
@Service
public class DeleteAccountService {

    public static final String REQUIRED_CONFIRMATION = "DELETE";

    private final AccountRepository accountRepository;

    public DeleteAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void deleteAccount(OwnerId owner, String confirmation) {
        if (!REQUIRED_CONFIRMATION.equals(confirmation)) {
            throw new ConfirmationRequiredException();
        }
        accountRepository.deleteById(owner.value());
    }
}
