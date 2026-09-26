package com.financialgps.application.account.port.in;

import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.OwnerId;

/** Use case: read the owner's own account summary (GET /account/me). */
public interface GetAccount {

    AccountView me(OwnerId owner);
}
