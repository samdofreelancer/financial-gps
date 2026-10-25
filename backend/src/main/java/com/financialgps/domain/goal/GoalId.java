package com.financialgps.domain.goal;

import java.util.Objects;
import java.util.UUID;

/** Identity of a goal aggregate. Null on a not-yet-persisted goal; assigned by the store on save. */
public record GoalId(UUID value) {

    public GoalId {
        Objects.requireNonNull(value, "value");
    }

    public static GoalId of(UUID value) {
        return new GoalId(value);
    }
}
