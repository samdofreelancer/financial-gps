package com.financialgps.application.account.port.in;

import com.financialgps.domain.model.OwnerId;

/** Use case: change the owner's password (T2). */
public interface ChangePassword {

    /**
     * @throws com.financialgps.application.account.InvalidCredentialsException when the current
     *         password does not match (identical body to login — never hints which part failed)
     * @throws com.financialgps.application.account.PasswordPolicyViolationException when the new
     *         password violates FR-005
     */
    void change(OwnerId owner, String currentPassword, String newPassword);
}
