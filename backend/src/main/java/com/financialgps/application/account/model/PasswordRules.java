package com.financialgps.application.account.model;

/**
 * FR-005 password policy rules as a pure application configuration value. The values are bound from
 * {@code financial.auth.password.*} in infrastructure; no application type reads Spring
 * configuration directly (plan Phase 2 step 3).
 */
public record PasswordRules(int minLength, boolean requireLetter, boolean requireDigit, int maxLength) {
}
