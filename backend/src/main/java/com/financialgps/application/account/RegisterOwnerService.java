package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Registration flow (plan §Security flow/Register):
 * 1. password policy — 422, independent of email existence (research §6);
 * 2. duplicate email (case-insensitive) — generic 409, no existence hint (FR-004);
 * 3. BCrypt hash + insert (SC-002);
 * 4. the API layer signs the new owner in immediately (auto sign-in, US1).
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
        accountRepository.save(account);
        return new AccountView(account.getId(), account.getEmail(), account.getCreatedAt());
    }
}
