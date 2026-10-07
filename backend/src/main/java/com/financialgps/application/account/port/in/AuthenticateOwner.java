package com.financialgps.application.account.port.in;

import com.financialgps.application.account.model.AccountView;

/**
 * Use case: authenticate an owner (US2). Both failure branches (unknown email, wrong password)
 * surface as ONE identical application outcome so the API renders one identical 401 body — the
 * anti-enumeration guarantee (FR-002, research §6).
 */
public interface AuthenticateOwner {

    AccountView authenticate(String email, String password);
}
