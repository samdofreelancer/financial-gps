package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import com.financialgps.platform.security.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T006 — register flow: policy first, generic conflict branch, BCrypt-only storage (SC-002). */
class RegisterOwnerServiceTest {

    private AccountRepository accountRepository;
    private RegisterOwnerService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        AuthProperties properties = new AuthProperties(Duration.ofMinutes(30), 12,
                new AuthProperties.Cookie(true, "lax"),
                new AuthProperties.Password(10, true, true, 128));
        service = new RegisterOwnerService(accountRepository,
                new PasswordPolicy(properties), new BCryptPasswordHasher(properties));
    }

    @Test
    void storesBcryptHashNeverPlaintext() {
        when(accountRepository.existsByLowerEmail(anyString())).thenReturn(false);

        AccountView view = service.register("User@Example.com", "correct horse battery1");

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).saveAndFlush(captor.capture());
        AccountEntity saved = captor.getValue();
        assertThat(saved.getPasswordHash())
                .startsWith("$2")
                .isNotEqualTo("correct horse battery1")
                .hasSizeGreaterThan(50);
        assertThat(saved.getRole()).isEqualTo("OWNER");
        assertThat(view.email()).isEqualTo("User@Example.com");
    }

    @Test
    void looksUpEmailCaseInsensitively() {
        when(accountRepository.existsByLowerEmail(anyString())).thenReturn(false);
        service.register("  User@Example.COM ", "correct horse battery1");
        verify(accountRepository).existsByLowerEmail("User@Example.COM");
    }

    @Test
    void policyViolationComesBeforeExistenceCheck() {
        assertThatThrownBy(() -> service.register("User@Example.com", "short"))
                .isInstanceOf(PasswordPolicyViolationException.class);
        verify(accountRepository, never()).existsByLowerEmail(anyString());
        verify(accountRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void concurrentInsertLosesTheRaceButStillAnswersTheConflictContract() {
        when(accountRepository.existsByLowerEmail(anyString())).thenReturn(false); // both requests saw "free"
        when(accountRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(AccountEntity.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"ux_account_email_lower\""));

        assertThatThrownBy(() -> service.register("User@Example.com", "correct horse battery1"))
                .as("a lost unique-index race is a conflict, not an infrastructure error")
                .isInstanceOf(RegistrationConflictException.class)
                .isNotInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageNotContaining("duplicate key");
    }

    @Test
    void duplicateEmailThrowsGenericConflict() {
        when(accountRepository.existsByLowerEmail(anyString())).thenReturn(true);
        assertThatThrownBy(() -> service.register("User@Example.com", "correct horse battery1"))
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessageNotContaining("taken")
                .hasMessageNotContaining("exists");
        verify(accountRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void storedHashVerifiesAgainstRawPassword() {
        when(accountRepository.existsByLowerEmail(anyString())).thenReturn(false);
        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);

        service.register("User@Example.com", "correct horse battery1");
        verify(accountRepository).saveAndFlush(captor.capture());

        AuthProperties properties = new AuthProperties(Duration.ofMinutes(30), 12,
                new AuthProperties.Cookie(true, "lax"),
                new AuthProperties.Password(10, true, true, 128));
        assertThat(new BCryptPasswordHasher(properties)
                .matches("correct horse battery1", captor.getValue().getPasswordHash())).isTrue();
        when(accountRepository.findById(captor.getValue().getId()))
                .thenReturn(Optional.of(captor.getValue()));
    }
}
