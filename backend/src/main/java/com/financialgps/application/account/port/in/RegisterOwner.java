package com.financialgps.application.account.port.in;

import com.financialgps.application.account.model.AccountView;

/**
 * Use case: register a new owner (US1). Implemented by
 * {@code application.account.usecase.RegisterOwnerUseCase} and invoked by the HTTP adapter only.
 */
public interface RegisterOwner {

    /**
     * @param email    display-case email as submitted; uniqueness is case-insensitive
     * @param password raw password, validated against the FR-005 policy before hashing
     * @return the created account; the adapter signs the principal in afterwards (auto sign-in)
     */
    AccountView register(String email, String password);
}
