package com.financialgps.application.account.usecase;

import com.financialgps.application.account.InvalidCredentialsException;
import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.port.in.AuthenticateOwner;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.port.out.AccountStore;
import com.financialgps.application.account.port.out.PasswordHashing;

/**
 * Login flow (plan §Security flow/Login). Both failure branches (unknown email, wrong password)
 * throw the SAME {@link InvalidCredentialsException} so the API renders one identical 401 body —
 * the anti-enumeration guarantee (FR-002, research §6).
 *
 * <p>Framework-free: the transaction is applied at the input-port boundary by an infrastructure
 * decorator (plan Phase 2 step 4).
 */
public final class AuthenticateOwnerUseCase implements AuthenticateOwner {

    private final AccountStore accounts;
    private final PasswordHashing passwordHashing;

    public AuthenticateOwnerUseCase(AccountStore accounts, PasswordHashing passwordHashing) {
        this.accounts = accounts;
        this.passwordHashing = passwordHashing;
    }

    @Override
    public AccountView authenticate(String email, String password) {
        String candidate = email == null ? "" : email.trim();
        AccountRecord account = accounts.findByEmail(candidate)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHashing.matches(password == null ? "" : password, account.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AccountView(account.id(), account.email(), account.createdAt());
    }
}
