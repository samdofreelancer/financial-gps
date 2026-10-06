package com.financialgps.application.account;

/**
 * A sensitive operation (export, delete) was attempted without the re-authentication
 * credential the T3 contract requires ({@code X-Reauth-Password} header or {@code password}
 * body field). Mapped to 400 VALIDATION_FAILED by the API adapter.
 */
public class ReauthRequiredException extends RuntimeException {

    public ReauthRequiredException(String message) {
        super(message);
    }
}
