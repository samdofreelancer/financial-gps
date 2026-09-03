package com.financialgps.application.account;

import com.financialgps.platform.security.AuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** T003 — FR-005 policy matrix with the plan's default configuration. */
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(new AuthProperties(
            Duration.ofMinutes(30), 12,
            new AuthProperties.Cookie(true, "lax"),
            new AuthProperties.Password(10, true, true, 128)));

    @Test
    void acceptsTenCharactersWithLetterAndDigit() {
        assertThat(policy.violations("correct horse battery1")).isEmpty();
    }

    @Test
    void rejectsTooShortPassword() {
        List<String> violations = policy.violations("short1a");
        assertThat(violations).anyMatch(v -> v.contains("10"));
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        List<String> violations = policy.violations("correct horse battery");
        assertThat(violations).anyMatch(v -> v.toLowerCase().contains("digit"));
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        List<String> violations = policy.violations("123456789012");
        assertThat(violations).anyMatch(v -> v.toLowerCase().contains("letter"));
    }

    @Test
    void rejectsPasswordOver128Characters() {
        String tooLong = "a1".repeat(65); // 130 chars
        List<String> violations = policy.violations(tooLong);
        assertThat(violations).anyMatch(v -> v.contains("128"));
    }

    @Test
    void rejectsNullAndBlankPasswords() {
        assertThat(policy.violations(null)).isNotEmpty();
        assertThat(policy.violations("")).isNotEmpty();
    }

    @Test
    void violationMessageListsActiveRequirements() {
        assertThat(policy.requirements())
                .contains("At least 10 characters", "At most 128 characters",
                        "At least one letter", "At least one digit");
    }

    @Test
    void validateOrThrowCarriesViolations() {
        try {
            policy.validateOrThrow("no-digit-password");
            throw new AssertionError("expected PasswordPolicyViolationException");
        } catch (PasswordPolicyViolationException exception) {
            assertThat(exception.getViolations()).isNotEmpty();
        }
    }
}
