package com.financialgps.application.account;

import com.financialgps.platform.security.AuthProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-005 password policy: min length, letter/digit classes, max length (hash-DoS guard).
 * Pure object — fully unit-tested (PasswordPolicyTest); violations explain every active
 * requirement so the user can fix the password (422 PASSWORD_POLICY_VIOLATION).
 */
@Component
public class PasswordPolicy {

    private final AuthProperties.Password rules;

    public PasswordPolicy(AuthProperties properties) {
        this.rules = properties.password();
    }

    /** All violations of the submitted password, or an empty list when it is acceptable. */
    public List<String> violations(String password) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            violations.add("Password is required.");
            return violations;
        }
        if (password.length() < rules.minLength()) {
            violations.add("Password must be at least " + rules.minLength() + " characters long.");
        }
        if (rules.requireLetter() && password.chars().noneMatch(Character::isLetter)) {
            violations.add("Password must contain at least one letter.");
        }
        if (rules.requireDigit() && password.chars().noneMatch(Character::isDigit)) {
            violations.add("Password must contain at least one digit.");
        }
        if (password.length() > rules.maxLength()) {
            violations.add("Password must be at most " + rules.maxLength() + " characters long.");
        }
        return violations;
    }

    /** The requirements currently in force — used to render the 422 problem detail. */
    public List<String> requirements() {
        List<String> requirements = new ArrayList<>();
        requirements.add("At least " + rules.minLength() + " characters");
        requirements.add("At most " + rules.maxLength() + " characters");
        if (rules.requireLetter()) {
            requirements.add("At least one letter");
        }
        if (rules.requireDigit()) {
            requirements.add("At least one digit");
        }
        return requirements;
    }

    public void validateOrThrow(String password) {
        List<String> violations = violations(password);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
    }
}
