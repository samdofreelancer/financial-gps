package com.financialgps.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SC-002 — the production {@code PasswordHashing} adapter stores a one-way BCrypt hash with the
 * configured cost factor and never the plaintext. Kept in the infrastructure lane: the account use
 * cases are tested against a fake hashing port instead.
 */
class BCryptPasswordHasherTest {

    private static final String PASSWORD = "correct horse battery1";

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(12);

    @Test
    void hashIsBcryptWithTheConfiguredCostAndNeverThePlaintext() {
        String hash = hasher.hash(PASSWORD);

        assertThat(hash).startsWith("$2a$12$").hasSizeGreaterThan(50);
        assertThat(hash).isNotEqualTo(PASSWORD).doesNotContain(PASSWORD);
    }

    @Test
    void hashVerifiesAgainstTheRawPassword() {
        assertThat(hasher.matches(PASSWORD, hasher.hash(PASSWORD))).isTrue();
        assertThat(hasher.matches("wrong password 99", hasher.hash(PASSWORD))).isFalse();
    }

    @Test
    void costFactorComesFromTheWiringValue() {
        assertThat(new BCryptPasswordHasher(4).hash(PASSWORD)).startsWith("$2a$04$");
    }
}
