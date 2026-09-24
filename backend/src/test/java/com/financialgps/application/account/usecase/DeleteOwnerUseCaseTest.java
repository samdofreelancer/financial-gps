package com.financialgps.application.account.usecase;

import com.financialgps.application.account.ConfirmationRequiredException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.testsupport.FakeAccountStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-012: only the exact confirmation unlocks the irreversible delete. The use case itself is
 * framework-free — the transaction boundary is applied by {@code infrastructure.configuration} and
 * asserted by {@code UseCaseTransactionsTest} (plan Phase 2 step 4).
 */
class DeleteOwnerUseCaseTest {

    private FakeAccountStore accounts;
    private DeleteOwnerUseCase useCase;
    private OwnerId owner;

    @BeforeEach
    void setUp() {
        accounts = new FakeAccountStore();
        useCase = new DeleteOwnerUseCase(accounts);
        AccountRecord stored = accounts.insert(new AccountRecord(null, "user@example.com",
                "$fake$hash", "OWNER", Instant.parse("2026-08-25T10:00:00Z")));
        owner = new OwnerId(stored.id());
    }

    @Test
    void rejectsMissingConfirmation() {
        assertThatThrownBy(() -> useCase.delete(owner, null))
                .isInstanceOf(ConfirmationRequiredException.class);
        assertThat(accounts.size()).isEqualTo(1);
    }

    @Test
    void rejectsWrongConfirmation() {
        assertThatThrownBy(() -> useCase.delete(owner, "delete"))
                .isInstanceOf(ConfirmationRequiredException.class);
        assertThatThrownBy(() -> useCase.delete(owner, "DELETE "))
                .isInstanceOf(ConfirmationRequiredException.class);
        assertThat(accounts.size()).as("nothing was deleted").isEqualTo(1);
    }

    @Test
    void exactConfirmationDeletesTheAccountRow() {
        assertThatCode(() -> useCase.delete(owner, DeleteOwnerUseCase.REQUIRED_CONFIRMATION))
                .doesNotThrowAnyException();
        assertThat(accounts.size()).isZero();
    }
}
