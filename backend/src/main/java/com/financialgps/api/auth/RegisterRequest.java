package com.financialgps.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Registration request (US1). Password quality is judged by PasswordPolicy (422), not here. */
public record RegisterRequest(
        @NotBlank @Email String email,
        String password) {
}
