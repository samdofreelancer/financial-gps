package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** T017 — confirmation gate: only the exact confirmation unlocks the irreversible delete. */
class DeleteAccountServiceTest {

    private AccountRepository accountRepository;
    private DeleteAccountService service;
    private OwnerId owner;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        service = new DeleteAccountService(accountRepository);
        owner = new OwnerId(UUID.randomUUID());
    }

    @Test
    void rejectsMissingConfirmation() {
        assertThatThrownBy(() -> service.deleteAccount(owner, null))
                .isInstanceOf(ConfirmationRequiredException.class);
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    void rejectsWrongConfirmation() {
        assertThatThrownBy(() -> service.deleteAccount(owner, "delete"))
                .isInstanceOf(ConfirmationRequiredException.class);
        assertThatThrownBy(() -> service.deleteAccount(owner, "DELETE "))
                .isInstanceOf(ConfirmationRequiredException.class);
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    void exactConfirmationDeletesAccountRow() {
        assertThatCode(() -> service.deleteAccount(owner, "DELETE")).doesNotThrowAnyException();
        verify(accountRepository).deleteById(owner.value());
    }
}
