package com.financialgps.infrastructure.persistence.account;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.port.out.AccountStore;
import com.financialgps.application.account.port.out.DuplicateAccountException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA adapter for {@link AccountStore}. The {@code AccountEntity} and its Spring Data repository are
 * private collaborators of this class: they never cross into the application lane (plan Phase 2
 * step 1).
 *
 * <p>Email lookups go through {@code lower(email)} (research §5, FR-001/FR-004). The unique-index
 * race is translated into {@link DuplicateAccountException} so the use case can answer the documented
 * conflict contract instead of leaking a persistence error (FR-004).
 */
@Component
class JpaAccountStore implements AccountStore {

    private final AccountRepository accounts;

    JpaAccountStore(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<AccountRecord> findById(OwnerId owner) {
        return accounts.findById(owner.value()).map(JpaAccountStore::toRecord);
    }

    @Override
    public Optional<AccountRecord> findByEmail(String email) {
        return accounts.findByLowerEmail(email).map(JpaAccountStore::toRecord);
    }

    @Override
    public boolean emailExists(String email) {
        return accounts.existsByLowerEmail(email);
    }

    @Override
    public AccountRecord insert(AccountRecord account) {
        AccountEntity entity = new AccountEntity(account.email(), account.passwordHash(),
                account.role(), account.createdAt());
        try {
            // saveAndFlush, not save: the unique index has to reject the row *here*, inside this
            // try, instead of failing later at commit time where no contract can catch it.
            return toRecord(accounts.saveAndFlush(entity));
        } catch (DataIntegrityViolationException lostRace) {
            throw new DuplicateAccountException(lostRace);
        }
    }

    @Override
    public void delete(OwnerId owner) {
        accounts.deleteById(owner.value());
    }

    private static AccountRecord toRecord(AccountEntity entity) {
        return new AccountRecord(entity.getId(), entity.getEmail(), entity.getPasswordHash(),
                entity.getRole(), entity.getCreatedAt());
    }
}
