package com.financialgps.application.account.usecase;

import com.financialgps.application.account.AuthRequiredException;
import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.in.GetAccount;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.port.out.AccountStore;

/**
 * Read-only account query for the owner themself (GET /account/me). A live session whose account row
 * no longer exists is treated as an unauthenticated request, exactly as before the refactor.
 */
public final class GetAccountUseCase implements GetAccount {

    private final AccountStore accounts;

    public GetAccountUseCase(AccountStore accounts) {
        this.accounts = accounts;
    }

    @Override
    public AccountView me(OwnerId owner) {
        AccountRecord account = accounts.findById(owner).orElseThrow(AuthRequiredException::new);
        return new AccountView(account.id(), account.email(), account.createdAt());
    }
}
