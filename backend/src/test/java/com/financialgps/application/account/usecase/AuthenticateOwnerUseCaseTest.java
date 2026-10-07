package com.financialgps.application.account.usecase;

import com.financialgps.application.account.InvalidCredentialsException;
import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.PasswordPolicy;
import com.financialgps.application.account.model.PasswordRules;
import com.financialgps.testsupport.FakeAccountStore;
import com.financialgps.testsupport.FakePasswordHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-002: one identical failure for unknown email and wrong password (anti-enumeration, research §6),
 * and a successful match on the hashed credential — all against fake OUTPUT PORTS.
 */
class AuthenticateOwnerUseCaseTest {

    private static final String PASSWORD = "correct horse battery1";

    private FakeAccountStore accounts;
    private FakePasswordHashing hashing;
    private AuthenticateOwnerUseCase useCase;
    private AccountView seeded;

    @BeforeEach
    void setUp() {
        accounts = new FakeAccountStore();
        hashing = new FakePasswordHashing();
        useCase = new AuthenticateOwnerUseCase(accounts, hashing);
        seeded = new RegisterOwnerUseCase(accounts, new PasswordPolicy(new PasswordRules(10, true, true, 128)),
                hashing).register("User@Example.com", PASSWORD);
    }

    @Test
    void authenticatesWithCorrectPassword() {
        AccountView view = useCase.authenticate("user@example.com", PASSWORD);

        assertThat(view.id()).isEqualTo(seeded.id());
        assertThat(view.email()).isEqualTo("User@Example.com");
    }

    @Test
    void unknownEmailFails() {
        assertThatThrownBy(() -> useCase.authenticate("ghost@example.com", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void wrongPasswordFails() {
        assertThatThrownBy(() -> useCase.authenticate("user@example.com", "wrong password 99"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void unknownEmailAndWrongPasswordProduceIdenticalFailure() {
        InvalidCredentialsException unknownEmail = org.assertj.core.api.Assertions
                .catchThrowableOfType(() -> useCase.authenticate("ghost@example.com", PASSWORD),
                        InvalidCredentialsException.class);
        InvalidCredentialsException wrongPassword = org.assertj.core.api.Assertions
                .catchThrowableOfType(() -> useCase.authenticate("user@example.com", "wrong password 99"),
                        InvalidCredentialsException.class);

        assertThat(unknownEmail).isNotNull();
        assertThat(wrongPassword).isNotNull();
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThat(unknownEmail.getClass()).isEqualTo(wrongPassword.getClass());
    }

    @Test
    void nullPasswordCannotMatchAStoredHash() {
        assertThatThrownBy(() -> useCase.authenticate("user@example.com", null))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
