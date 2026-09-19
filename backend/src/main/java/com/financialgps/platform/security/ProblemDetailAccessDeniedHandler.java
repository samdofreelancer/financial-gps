package com.financialgps.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import java.io.IOException;

/**
 * ProblemDetail for 403s. CSRF failures (missing/invalid {@code X-XSRF-TOKEN} on a state-changing
 * request) map to {@code CSRF_INVALID}; every other denial maps to a generic {@code FORBIDDEN}
 * (plan §Error catalogue, T002).
 */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (accessDeniedException instanceof MissingCsrfTokenException
                || accessDeniedException instanceof InvalidCsrfTokenException) {
            ProblemJson.write(response, HttpServletResponse.SC_FORBIDDEN, "CSRF_INVALID",
                    "Invalid CSRF token",
                    "Include the X-XSRF-TOKEN header with the value of the XSRF-TOKEN cookie.");
            return;
        }
        ProblemJson.write(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "Access denied", "You do not have access to this resource.");
    }
}
