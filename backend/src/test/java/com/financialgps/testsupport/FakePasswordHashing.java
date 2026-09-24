package com.financialgps.testsupport;

import com.financialgps.application.account.port.out.PasswordHashing;

/**
 * Deterministic in-memory {@link PasswordHashing} test double: it records how often a hash was
 * computed and produces a NON-REVERSIBLE digest, so a use-case test can prove policy ordering and
 * SC-002 storage behaviour without paying BCrypt's cost. BCrypt itself is covered by
 * {@code BCryptPasswordHasherTest} in the infrastructure lane.
 */
public final class FakePasswordHashing implements PasswordHashing {

    private static final String PREFIX = "$fake$";

    private int hashCount;

    public int hashCount() {
        return hashCount;
    }

    private static String digest(String rawPassword) {
        String raw = rawPassword == null ? "" : rawPassword;
        return PREFIX + Integer.toHexString(raw.hashCode()) + "$" + raw.length();
    }

    @Override
    public String hash(String rawPassword) {
        hashCount++;
        return digest(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return digest(rawPassword).equals(passwordHash);
    }
}
