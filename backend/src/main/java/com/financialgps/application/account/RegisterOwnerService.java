package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Registration flow (plan §Security flow/Register):
 * 1. password policy — 422, independent of email existence (research §6);
 * 2. duplicate email (case-insensitive) — generic 409, no existence hint (FR-004);
 * 3. BCrypt hash + insert (SC-002);
 * 4. the API layer signs the new owner in immediately (auto sign-in, US1).
 *
 * <p>Step 2's pre-check is a courtesy fast path (it avoids hashing for an obvious duplicate); it
 * is <em>not</em> the uniqueness authority, because two concurrent requests can both clear it. The
 * authority is the {@code ux_account_email_lower} index, and losing that race is translated back
 * into the same 409 contract — a duplicate is a business outcome, not an infrastructure error.
 */
@Service
public class RegisterOwnerService {

    public static final String OWNER_ROLE = "OWNER";

    private final AccountRepository accountRepository;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHasher passwordHasher;

    public RegisterOwnerService(AccountRepository accountRepository,
                                PasswordPolicy passwordPolicy,
                                PasswordHasher passwordHasher) {
        this.accountRepository = accountRepository;
        this.passwordPolicy = passwordPolicy;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public AccountView register(String email, String password) {
        passwordPolicy.validateOrThrow(password);

        String displayEmail = email == null ? "" : email.trim();
        if (accountRepository.existsByLowerEmail(displayEmail)) {
            throw new RegistrationConflictException();
        }

        AccountEntity account = new AccountEntity(
                displayEmail, passwordHasher.hash(password), OWNER_ROLE, Instant.now());
        try {
            // saveAndFlush, not save: the unique index has to reject the row *here*, inside this
            // try, instead of failing later at commit time where no contract can catch it.
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException lostRace) {
            throw new RegistrationConflictException(lostRace);
        }
        return new AccountView(account.getId(), account.getEmail(), account.getCreatedAt());
    }
}
