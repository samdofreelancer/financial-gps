package com.financialgps.application.account;

/**
 * One-way password hashing seam (research §3). The default implementation is BCrypt; a future
 * swap (e.g. Argon2id) changes nothing else in the codebase.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
