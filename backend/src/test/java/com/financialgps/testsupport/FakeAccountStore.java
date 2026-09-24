package com.financialgps.testsupport;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.port.out.AccountStore;
import com.financialgps.application.account.port.out.DuplicateAccountException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory {@link AccountStore} test double.
 *
 * <p>Use-case unit tests run against OUTPUT PORTS, so they need no Spring context, no JPA and no
 * database (plan Phase 1 exit gate: "use-case unit tests run with fake output ports"). Case-
 * insensitive email matching and the unique-index race are modelled here so the use cases can be
 * driven into their real interleavings.
 */
public final class FakeAccountStore implements AccountStore {

    private final Map<UUID, AccountRecord> rows = new LinkedHashMap<>();
    private boolean duplicateOnNextInsert;

    /** Simulates a competing transaction committing between the pre-check and this insert. */
    public void raceOnNextInsert() {
        this.duplicateOnNextInsert = true;
    }

    public int size() {
        return rows.size();
    }

    @Override
    public Optional<AccountRecord> findById(OwnerId owner) {
        return Optional.ofNullable(rows.get(owner.value()));
    }

    @Override
    public Optional<AccountRecord> findByEmail(String email) {
        return rows.values().stream()
                .filter(row -> row.email().toLowerCase(Locale.ROOT).equals(lower(email)))
                .findFirst();
    }

    @Override
    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public AccountRecord insert(AccountRecord account) {
        if (duplicateOnNextInsert) {
            duplicateOnNextInsert = false;
            throw new DuplicateAccountException(new IllegalStateException("simulated lost race"));
        }
        if (emailExists(account.email())) {
            throw new DuplicateAccountException(new IllegalStateException("simulated unique index"));
        }
        AccountRecord stored = new AccountRecord(
                account.id() == null ? UUID.randomUUID() : account.id(),
                account.email(), account.passwordHash(), account.role(), account.createdAt());
        rows.put(stored.id(), stored);
        return stored;
    }

    @Override
    public void delete(OwnerId owner) {
        rows.remove(owner.value());
    }

    private static String lower(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
