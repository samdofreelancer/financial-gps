package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import com.financialgps.platform.security.AuthRequiredException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only account queries for the owner themself (GET /account/me). */
@Service
public class AccountReader {

    private final AccountRepository accountRepository;

    public AccountReader(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public AccountView me(com.financialgps.application.account.OwnerId owner) {
        AccountEntity account = accountRepository.findById(owner.value())
                .orElseThrow(AuthRequiredException::new);
        return new AccountView(account.getId(), account.getEmail(), account.getCreatedAt());
    }
}
