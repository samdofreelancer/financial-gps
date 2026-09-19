package com.financialgps.application.account;

import com.financialgps.platform.security.AuthProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt with the configured cost factor (default 12, SC-002). No plaintext is ever stored. */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasher(AuthProperties properties) {
        this.encoder = new BCryptPasswordEncoder(properties.bcryptStrength());
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
