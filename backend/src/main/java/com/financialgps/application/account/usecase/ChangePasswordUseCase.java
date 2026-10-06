package com.financialgps.application.account.usecase;

import com.financialgps.application.account.InvalidCredentialsException;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.model.PasswordPolicy;
import com.financialgps.application.account.port.in.AuthenticateOwner;
import com.financialgps.application.account.port.in.ChangePassword;
import com.financialgps.application.account.port.out.AccountStore;
import com.financialgps.application.account.port.out.PasswordHashing;
import com.financialgps.domain.model.OwnerId;

/**
 * Change-password flow (T2). Order matters: authenticate with the current password BEFORE any
 * mutation (anti-enumeration: the failure is the SAME InvalidCredentialsException as login), then
 * enforce FR-005 on the new password (422), then hash and persist. Session mechanics stay in the
 * API adapter — this use case only owns the credential row.
 */
public final class ChangePasswordUseCase implements ChangePassword {

    private final AccountStore accounts;
    private final AuthenticateOwner authenticateOwner;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHashing passwordHashing;

    public ChangePasswordUseCase(AccountStore accounts, AuthenticateOwner authenticateOwner,
                                 PasswordPolicy passwordPolicy, PasswordHashing passwordHashing) {
        this.accounts = accounts;
        this.authenticateOwner = authenticateOwner;
        this.passwordPolicy = passwordPolicy;
        this.passwordHashing = passwordHashing;
    }

    @Override
    public void change(OwnerId owner, String currentPassword, String newPassword) {
        AccountRecord account = accounts.findById(owner).orElseThrow(InvalidCredentialsException::new);
        authenticateOwner.authenticate(account.email(), currentPassword);
        passwordPolicy.validateOrThrow(newPassword);
        accounts.updatePasswordHash(owner, passwordHashing.hash(newPassword));
    }
}
