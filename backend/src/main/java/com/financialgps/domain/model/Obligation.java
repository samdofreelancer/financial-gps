package com.financialgps.domain.model;

import java.util.Objects;

/**
 * Shared-kernel value object: a mandatory monthly cash commitment that reduces Net Cash Flow
 * one-for-one (today: an ACTIVE debt's minimum payment, mapped at the debt→profile port
 * boundary).
 *
 * <p>Bounded contexts never exchange aggregates — the profile context and the engine depend
 * only on this kernel type, never on the debt context's {@code Debt} aggregate.
 */
public record Obligation(Money amount) {

    public Obligation {
        Objects.requireNonNull(amount, "amount");
    }
}
