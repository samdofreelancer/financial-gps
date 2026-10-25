package com.financialgps.application.profile.usecase;

/** Application-level validation failure: mapped to 400 VALIDATION_FAILED by the API adapter. */
public class ProfileValidationException extends RuntimeException {

    private final String code;

    public ProfileValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
