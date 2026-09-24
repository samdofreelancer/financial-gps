package com.financialgps.application.account.port.out;

/**
 * One-way password hashing port (research §3). The production adapter is BCrypt with the configured
 * cost factor (SC-002); a future swap (e.g. Argon2id) changes nothing else in the codebase. No
 * plaintext is ever stored.
 */
public interface PasswordHashing {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
