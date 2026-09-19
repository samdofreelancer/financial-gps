package com.financialgps.domain.model;

/** Typed domain validation failure with a stable machine code (mapped to ProblemDetail upstream). */
public class DomainValidationException extends RuntimeException {

    private final String code;

    public DomainValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
