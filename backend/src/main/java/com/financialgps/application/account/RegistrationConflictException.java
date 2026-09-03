package com.financialgps.application.account;

/**
 * Generic duplicate-email rejection (FR-004). The API layer must render one identical
 * {@code 409 REGISTRATION_FAILED} body for every email-based rejection — never an existence hint.
 */
public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException() {
        super("Registration failed");
    }
}
