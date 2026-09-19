package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login flow (plan §Security flow/Login). Both failure branches (unknown email, wrong password)
 * throw the SAME {@link InvalidCredentialsException} so the API renders one identical 401 body —
 * the anti-enumeration guarantee (FR-002, research §6).
 */
@Service
public class AuthenticateOwnerService {

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticateOwnerService(AccountRepository accountRepository, PasswordHasher passwordHasher) {
        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional(readOnly = true)
    public AccountView authenticate(String email, String password) {
        String candidate = email == null ? "" : email.trim();
        AccountEntity account = accountRepository.findByLowerEmail(candidate)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(password == null ? "" : password, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AccountView(account.getId(), account.getEmail(), account.getCreatedAt());
    }
}
