package com.financialgps.application.account;

/**
 * Generic duplicate-email rejection (FR-004). The API layer must render one identical
 * {@code 409 REGISTRATION_FAILED} body for every email-based rejection — never an existence hint.
 */
public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException() {
        super("Registration failed");
    }

    /**
     * Same generic message, with the real cause kept server-side for diagnostics. The cause is
     * never serialized into the response body (the advice renders a fixed detail), so no
     * existence hint and no persistence detail can leak (FR-004).
     */
    public RegistrationConflictException(Throwable cause) {
        super("Registration failed", cause);
    }
}
