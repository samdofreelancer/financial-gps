package com.financialgps.api.account;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /account/password body (T2): proves knowledge of the current password and proposes the new
 * one (FR-005 is enforced by the application lane, answered as 422).
 */
public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
