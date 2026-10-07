package com.financialgps.infrastructure.security;

import com.financialgps.application.account.port.out.PasswordHashing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt adapter for {@link PasswordHashing} with the configured cost factor (default 12, SC-002).
 * The only class in the codebase that knows the hashing algorithm; swapped by changing the wiring,
 * not the use cases.
 */
public final class BCryptPasswordHasher implements PasswordHashing {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasher(int bcryptStrength) {
        this.encoder = new BCryptPasswordEncoder(bcryptStrength);
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
