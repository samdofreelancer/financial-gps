package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import com.financialgps.platform.security.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T008 — login flow: one identical failure for unknown email and wrong password
 * (anti-enumeration, research §6), BCrypt match on success.
 */
class AuthenticateOwnerServiceTest {

    private static final String PASSWORD = "correct horse battery1";

    private AccountRepository accountRepository;
    private AuthenticateOwnerService service;
    private AccountEntity seeded;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        AuthProperties properties = new AuthProperties(Duration.ofMinutes(30), 12,
                new AuthProperties.Cookie(true, "lax"),
                new AuthProperties.Password(10, true, true, 128));
        BCryptPasswordHasher hasher = new BCryptPasswordHasher(properties);
        service = new AuthenticateOwnerService(accountRepository, hasher);

        seeded = new AccountEntity("User@Example.com", hasher.hash(PASSWORD), "OWNER",
                java.time.Instant.now());
        // id is assigned by JPA in production; set via reflection for the unit test.
        org.springframework.test.util.ReflectionTestUtils.setField(seeded, "id", UUID.randomUUID());
    }

    @Test
    void authenticatesWithCorrectPassword() {
        when(accountRepository.findByLowerEmail(anyString())).thenReturn(Optional.of(seeded));

        AccountView view = service.authenticate("user@example.com", PASSWORD);

        assertThat(view.id()).isEqualTo(seeded.getId());
        assertThat(view.email()).isEqualTo("User@Example.com");
    }

    @Test
    void unknownEmailFails() {
        when(accountRepository.findByLowerEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate("ghost@example.com", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void wrongPasswordFails() {
        when(accountRepository.findByLowerEmail(anyString())).thenReturn(Optional.of(seeded));

        assertThatThrownBy(() -> service.authenticate("user@example.com", "wrong password 99"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void unknownEmailAndWrongPasswordProduceIdenticalFailure() {
        when(accountRepository.findByLowerEmail("ghost@example.com")).thenReturn(Optional.empty());
        InvalidCredentialsException unknownEmail = org.assertj.core.api.Assertions
                .catchThrowableOfType(() -> service.authenticate("ghost@example.com", PASSWORD),
                        InvalidCredentialsException.class);

        when(accountRepository.findByLowerEmail("user@example.com")).thenReturn(Optional.of(seeded));
        InvalidCredentialsException wrongPassword = org.assertj.core.api.Assertions
                .catchThrowableOfType(() -> service.authenticate("user@example.com", "wrong password 99"),
                        InvalidCredentialsException.class);

        assertThat(unknownEmail).isNotNull();
        assertThat(wrongPassword).isNotNull();
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThat(unknownEmail.getClass()).isEqualTo(wrongPassword.getClass());
    }
}
