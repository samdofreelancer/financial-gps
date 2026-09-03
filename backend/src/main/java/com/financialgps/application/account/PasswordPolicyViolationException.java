package com.financialgps.application.account;

/** Thrown when a submitted password violates the active FR-005 policy; carries the violations. */
public class PasswordPolicyViolationException extends RuntimeException {

    private final java.util.List<String> violations;

    public PasswordPolicyViolationException(java.util.List<String> violations) {
        super("Password does not meet the requirements");
        this.violations = java.util.List.copyOf(violations);
    }

    public java.util.List<String> getViolations() {
        return violations;
    }
}
