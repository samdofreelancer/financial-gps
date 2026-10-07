package com.financialgps.domain.debt;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a debt aggregate. Null on a not-yet-persisted debt; assigned by the store on save.
 */
public record DebtId(UUID value) {

    public DebtId {
        Objects.requireNonNull(value, "value");
    }

    public static DebtId of(UUID value) {
        return new DebtId(value);
    }
}
