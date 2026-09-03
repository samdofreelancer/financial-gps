package com.financialgps.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Uniform {@code 401 AUTH_REQUIRED} problem for every request reaching a protected route without
 * a valid session (plan §Error catalogue, SC-005, FR-006).
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        ProblemJson.write(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_REQUIRED",
                "Authentication required", "Sign in to access this resource.");
    }
}
