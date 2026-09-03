package com.financialgps.api.account;

import jakarta.validation.constraints.NotBlank;

/** DELETE /account body (FR-012): must carry the exact {@code confirmation:"DELETE"}. */
public record DeleteAccountRequest(@NotBlank String confirmation) {
}
