package com.financialgps.api.account;

import jakarta.validation.constraints.NotBlank;

/**
 * DELETE /account body (FR-012): must carry the exact {@code confirmation:"DELETE"} token AND the
 * account password for T3 re-authentication of the irreversible operation.
 */
public record DeleteAccountRequest(@NotBlank String confirmation, @NotBlank String password) {
}
