package com.financialgps.application.goal.usecase;

/** Application-level validation failure → 400 VALIDATION_FAILED. */
public class GoalValidationException extends RuntimeException {

    private final String code;

    public GoalValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
