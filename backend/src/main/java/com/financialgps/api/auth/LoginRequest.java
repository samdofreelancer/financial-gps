package com.financialgps.api.auth;

import jakarta.validation.constraints.NotBlank;

/** Login request (US2). Failures render one identical 401 body (FR-002, research §6). */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password) {
}
