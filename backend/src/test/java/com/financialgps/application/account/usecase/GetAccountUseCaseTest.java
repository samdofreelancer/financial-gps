package com.financialgps.application.account.usecase;

import com.financialgps.application.account.AuthRequiredException;
import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.model.PasswordPolicy;
import com.financialgps.application.account.model.PasswordRules;
import com.financialgps.testsupport.FakeAccountStore;
import com.financialgps.testsupport.FakePasswordHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** GET /account/me: owner-scoped read, and a session whose account row vanished is unauthenticated. */
class GetAccountUseCaseTest {

    private FakeAccountStore accounts;
    private GetAccountUseCase useCase;
    private AccountView seeded;

    @BeforeEach
    void setUp() {
        accounts = new FakeAccountStore();
        useCase = new GetAccountUseCase(accounts);
        seeded = new RegisterOwnerUseCase(accounts,
                new PasswordPolicy(new PasswordRules(10, true, true, 128)),
                new FakePasswordHashing()).register("User@Example.com", "correct horse battery1");
    }

    @Test
    void returnsTheAccountOfTheGivenOwner() {
        AccountView view = useCase.me(new OwnerId(seeded.id()));

        assertThat(view.id()).isEqualTo(seeded.id());
        assertThat(view.email()).isEqualTo("User@Example.com");
        assertThat(view.createdAt()).isEqualTo(seeded.createdAt());
    }

    @Test
    void anotherOrUnknownOwnerIsUnauthenticated() {
        assertThatThrownBy(() -> useCase.me(new OwnerId(UUID.randomUUID())))
                .isInstanceOf(AuthRequiredException.class);
    }
}
