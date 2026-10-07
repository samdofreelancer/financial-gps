package com.financialgps.application.account;

/**
 * Thrown when an authenticated owner is required but the request carries no valid session
 * (FR-006). Raised by the platform {@code CurrentOwnerProvider} adapter and by account use cases
 * whose row vanished under a live session; the API advice renders it as {@code 401 AUTH_REQUIRED}.
 */
public class AuthRequiredException extends RuntimeException {

    public AuthRequiredException() {
        super("Authentication required");
    }
}
