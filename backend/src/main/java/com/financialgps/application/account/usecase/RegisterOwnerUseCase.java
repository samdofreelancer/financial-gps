package com.financialgps.application.account.usecase;

import com.financialgps.application.account.RegistrationConflictException;
import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.OwnerRole;
import com.financialgps.application.account.model.PasswordPolicy;
import com.financialgps.application.account.port.in.RegisterOwner;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.port.out.AccountStore;
import com.financialgps.application.account.port.out.DuplicateAccountException;
import com.financialgps.application.account.port.out.PasswordHashing;

import java.time.Instant;

/**
 * Registration flow (plan §Security flow/Register):
 * 1. password policy — 422, independent of email existence (research §6);
 * 2. duplicate email (case-insensitive) — generic 409, no existence hint (FR-004);
 * 3. password hash + insert (SC-002);
 * 4. the API adapter signs the new owner in immediately (auto sign-in, US1).
 *
 * <p>Step 2's pre-check is a courtesy fast path (it avoids hashing for an obvious duplicate); it is
 * <em>not</em> the uniqueness authority, because two concurrent requests can both clear it. The
 * authority is the case-insensitive unique index behind {@link AccountStore#insert}, and losing
 * that race is translated back into the same 409 contract — a duplicate is a business outcome, not
 * an infrastructure error.
 *
 * <p>Framework-free: no Spring stereotype, no transaction annotation, no JPA type. The transaction
 * is applied at the input-port boundary by an infrastructure decorator (plan Phase 2 step 4).
 */
public final class RegisterOwnerUseCase implements RegisterOwner {

    private final AccountStore accounts;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHashing passwordHashing;

    public RegisterOwnerUseCase(AccountStore accounts,
                                PasswordPolicy passwordPolicy,
                                PasswordHashing passwordHashing) {
        this.accounts = accounts;
        this.passwordPolicy = passwordPolicy;
        this.passwordHashing = passwordHashing;
    }

    @Override
    public AccountView register(String email, String password) {
        passwordPolicy.validateOrThrow(password);

        String displayEmail = email == null ? "" : email.trim();
        if (accounts.emailExists(displayEmail)) {
            throw new RegistrationConflictException();
        }

        AccountRecord candidate = new AccountRecord(null, displayEmail,
                passwordHashing.hash(password), OwnerRole.OWNER, Instant.now());
        AccountRecord saved;
        try {
            saved = accounts.insert(candidate);
        } catch (DuplicateAccountException lostRace) {
            throw new RegistrationConflictException(lostRace);
        }
        return new AccountView(saved.id(), saved.email(), saved.createdAt());
    }
}
