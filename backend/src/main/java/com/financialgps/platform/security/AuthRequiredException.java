package com.financialgps.platform.security;

/** Thrown when an authenticated owner is required but the request has no valid session. */
public class AuthRequiredException extends RuntimeException {

    public AuthRequiredException() {
        super("Authentication required");
    }
}
