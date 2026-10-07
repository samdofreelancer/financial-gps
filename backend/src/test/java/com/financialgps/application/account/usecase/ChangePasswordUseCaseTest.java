package com.financialgps.application.account.usecase;

import com.financialgps.application.account.InvalidCredentialsException;
import com.financialgps.application.account.PasswordPolicyViolationException;
import com.financialgps.application.account.model.PasswordPolicy;
import com.financialgps.application.account.model.PasswordRules;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.testsupport.FakeAccountStore;
import com.financialgps.testsupport.FakePasswordHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T2: change-password flow against fake OUTPUT PORTS — no Spring context, no JPA, no database.
 * Wrong current password → the SAME InvalidCredentialsException as login (anti-enumeration), weak
 * new password → PasswordPolicyViolationException BEFORE any hashing or storage mutation.
 */
class ChangePasswordUseCaseTest {

    private static final String CURRENT = "correct horse battery1";
    private static final String NEXT = "brand new passphrase 2";

    private FakeAccountStore accounts;
    private FakePasswordHashing hashing;
    private ChangePasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        accounts = new FakeAccountStore();
        hashing = new FakePasswordHashing();
        AuthenticateOwnerUseCase authenticate = new AuthenticateOwnerUseCase(accounts, hashing);
        useCase = new ChangePasswordUseCase(accounts, authenticate,
                new PasswordPolicy(new PasswordRules(10, true, true, 128)), hashing);
    }

    private OwnerId registeredOwner() {
        return new OwnerId(new RegisterOwnerUseCase(accounts,
                new PasswordPolicy(new PasswordRules(10, true, true, 128)), hashing)
                .register("user@example.com", CURRENT).id());
    }

    @Test
    void rotatesTheStoredHashAndKeepsTheAccountRow() {
        OwnerId owner = registeredOwner();
        int sizeBefore = accounts.size();

        useCase.change(owner, CURRENT, NEXT);

        assertThat(accounts.size()).isEqualTo(sizeBefore);
        assertThat(accounts.findById(owner)).hasValueSatisfying(stored -> {
            assertThat(hashing.matches(NEXT, stored.passwordHash())).isTrue();
            assertThat(hashing.matches(CURRENT, stored.passwordHash())).isFalse();
        });
    }

    @Test
    void wrongCurrentPasswordFailsLikeLoginAndTouchesNothing() {
        OwnerId owner = registeredOwner();
        int hashesBefore = hashing.hashCount();

        assertThatThrownBy(() -> useCase.change(owner, "wrong password 99", NEXT))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(accounts.findById(owner)).hasValueSatisfying(stored ->
                assertThat(hashing.matches(CURRENT, stored.passwordHash())).isTrue());
        assertThat(hashing.hashCount()).isEqualTo(hashesBefore);
    }

    @Test
    void unknownOwnerFailsLikeLogin() {
        assertThatThrownBy(() -> useCase.change(new OwnerId(UUID.randomUUID()), CURRENT, NEXT))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void weakNewPasswordRejectedBeforeHashing() {
        OwnerId owner = registeredOwner();
        int hashesBefore = hashing.hashCount();

        assertThatThrownBy(() -> useCase.change(owner, CURRENT, "short1"))
                .isInstanceOf(PasswordPolicyViolationException.class);

        assertThat(hashing.hashCount()).isEqualTo(hashesBefore);
        assertThat(accounts.findById(owner)).hasValueSatisfying(stored ->
                assertThat(hashing.matches(CURRENT, stored.passwordHash())).isTrue());
    }
}
