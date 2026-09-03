package com.financialgps.api.common;

import com.financialgps.application.account.ConfirmationRequiredException;
import com.financialgps.application.account.InvalidCredentialsException;
import com.financialgps.application.account.PasswordPolicyViolationException;
import com.financialgps.application.account.RegistrationConflictException;
import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.platform.security.AuthRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

/**
 * Shared RFC 7807 error mapping — the plan's §Error catalogue. Every body carries the machine
 * {@code code}; anti-revelation rules are honored here: {@code REGISTRATION_FAILED} and
 * {@code INVALID_CREDENTIALS} never hint at what was wrong.
 */
@RestControllerAdvice
public class ProblemDetailAdvice {

    @ExceptionHandler(AuthRequiredException.class)
    public ProblemDetail authRequired(AuthRequiredException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication required",
                "Sign in to access this resource.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail invalidCredentials(InvalidCredentialsException exception) {
        // Identical body for unknown email and wrong password (FR-002, AntiRevelationTest).
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials",
                "Email or password is incorrect.");
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ProblemDetail registrationConflict(RegistrationConflictException exception) {
        // Generic body — never an existence hint (FR-004).
        return problem(HttpStatus.CONFLICT, "REGISTRATION_FAILED", "Registration failed",
                "Registration failed. Try again with different details.");
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ProblemDetail passwordPolicy(PasswordPolicyViolationException exception) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY, "PASSWORD_POLICY_VIOLATION",
                "Password policy violation", String.join(" ", exception.getViolations()));
        problem.setProperty("violations", exception.getViolations());
        return problem;
    }

    @ExceptionHandler(ConfirmationRequiredException.class)
    public ProblemDetail confirmationRequired(ConfirmationRequiredException exception) {
        return problem(HttpStatus.BAD_REQUEST, "CONFIRMATION_REQUIRED", "Confirmation required",
                exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail resourceNotFound(ResourceNotFoundException exception) {
        // Missing or cross-owner: one indistinguishable body (FR-010).
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found",
                "Resource not found.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validationFailed(MethodArgumentNotValidException exception) {
        List<String> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(ProblemDetailAdvice::violationText)
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed",
                "Request body is invalid.");
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HandlerMethodValidationException.class})
    public ProblemDetail unreadable(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed",
                "Request body is invalid.");
    }

    private static String violationText(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private static ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }
}
