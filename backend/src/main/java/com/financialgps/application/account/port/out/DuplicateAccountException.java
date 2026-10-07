package com.financialgps.application.account.port.out;

/**
 * Raised by an {@link AccountStore} adapter when the case-insensitive unique email index rejects an
 * insert. Deliberately infrastructure-agnostic: the use case translates it into the documented
 * registration conflict, so no persistence detail (constraint name, SQL state) can leak (FR-004).
 */
public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException(Throwable cause) {
        super("An account with this email already exists", cause);
    }
}
