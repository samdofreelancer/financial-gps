package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirmed account deletion (FR-012, FR-014, SC-007): one hard delete of the {@code account} row
 * inside a single transaction; every owned row goes with it through
 * {@code owner_id REFERENCES account(id) ON DELETE CASCADE} (research §7). No soft-delete state.
 */
@Service
public class DeleteAccountService {

    public static final String REQUIRED_CONFIRMATION = "DELETE";

    private final AccountRepository accountRepository;

    public DeleteAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void deleteAccount(OwnerId owner, String confirmation) {
        if (!REQUIRED_CONFIRMATION.equals(confirmation)) {
            throw new ConfirmationRequiredException();
        }
        accountRepository.deleteById(owner.value());
    }
}
