package com.financialgps.application.account.usecase;

import com.financialgps.application.account.PasswordPolicyViolationException;
import com.financialgps.application.account.RegistrationConflictException;
import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.model.OwnerRole;
import com.financialgps.application.account.model.PasswordPolicy;
import com.financialgps.application.account.model.PasswordRules;
import com.financialgps.testsupport.FakeAccountStore;
import com.financialgps.testsupport.FakePasswordHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-001/FR-004/FR-005/SC-002: register flow against fake OUTPUT PORTS — no Spring context, no JPA,
 * no database. Policy first, generic conflict branch, and no plaintext ever handed to the store.
 */
class RegisterOwnerUseCaseTest {

    private FakeAccountStore accounts;
    private FakePasswordHashing hashing;
    private RegisterOwnerUseCase useCase;

    @BeforeEach
    void setUp() {
        accounts = new FakeAccountStore();
        hashing = new FakePasswordHashing();
        useCase = new RegisterOwnerUseCase(accounts, new PasswordPolicy(defaultRules()), hashing);
    }

    private static PasswordRules defaultRules() {
        return new PasswordRules(10, true, true, 128);
    }

    @Test
    void storesAHashNeverPlaintext() {
        AccountView view = useCase.register("User@Example.com", "correct horse battery1");

        assertThat(accounts.findByEmail("user@example.com")).hasValueSatisfying(stored -> {
            assertThat(stored.passwordHash()).isNotEqualTo("correct horse battery1");
            assertThat(stored.passwordHash()).doesNotContain("correct horse battery1");
            assertThat(stored.role()).isEqualTo(OwnerRole.OWNER);
        });
        assertThat(view.email()).isEqualTo("User@Example.com");
        assertThat(view.id()).isNotNull();
    }

    @Test
    void looksUpEmailCaseInsensitivelyAndTrimsTheDisplayValue() {
        useCase.register("  User@Example.COM ", "correct horse battery1");

        assertThat(accounts.emailExists("user@example.com")).isTrue();
        assertThat(accounts.findByEmail("USER@EXAMPLE.COM"))
                .hasValueSatisfying(stored -> assertThat(stored.email()).isEqualTo("User@Example.COM"));
    }

    @Test
    void policyViolationComesBeforeExistenceCheckAndBeforeHashing() {
        assertThatThrownBy(() -> useCase.register("User@Example.com", "short"))
                .isInstanceOf(PasswordPolicyViolationException.class);

        assertThat(hashing.hashCount()).isZero();
        assertThat(accounts.size()).isZero();
    }

    @Test
    void concurrentInsertLosesTheRaceButStillAnswersTheConflictContract() {
        accounts.raceOnNextInsert();

        assertThatThrownBy(() -> useCase.register("User@Example.com", "correct horse battery1"))
                .as("a lost unique-index race is a conflict, not an infrastructure error")
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessageNotContaining("duplicate key");
        assertThat(accounts.size()).isZero();
    }

    @Test
    void duplicateEmailThrowsGenericConflictWithoutExistenceHint() {
        useCase.register("User@Example.com", "correct horse battery1");

        assertThatThrownBy(() -> useCase.register("user@example.com", "correct horse battery1"))
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessageNotContaining("taken")
                .hasMessageNotContaining("exists");
        assertThat(accounts.size()).as("the duplicate never reached the store").isEqualTo(1);
    }

    @Test
    void registrationDependsOnPortsOnly() {
        // The use case holds no Spring, JPA or platform type: every collaborator is a port.
        assertThat(RegisterOwnerUseCase.class.getDeclaredFields())
                .allSatisfy(field -> assertThat(field.getType().getName())
                        .doesNotContain("springframework")
                        .doesNotContain("jakarta")
                        .doesNotContain("infrastructure"));
    }

    @Test
    void aFreshlyRegisteredOwnerCanBeFoundByItsAssignedId() {
        AccountView view = useCase.register("user@example.com", "correct horse battery1");

        assertThat(view.createdAt()).isNotNull();
        assertThat(accounts.findById(new OwnerId(view.id())))
                .hasValueSatisfying(stored -> assertThat(stored.email()).isEqualTo("user@example.com"));
    }
}
