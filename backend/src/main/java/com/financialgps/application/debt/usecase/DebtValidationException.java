package com.financialgps.application.debt.usecase;

/** Application-level validation failure: mapped to 400 VALIDATION_FAILED by the API adapter. */
public class DebtValidationException extends RuntimeException {

    private final String code;

    public DebtValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
