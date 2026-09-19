package com.financialgps.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/** Monthly money inflow with amount + source. Inactive items are excluded from calculation. */
public final class Income {

    private final Money amount;
    private final String source;
    private final boolean active;
    private final LocalDate effectiveFrom;

    public Income(Money amount, String source, boolean active, LocalDate effectiveFrom) {
        this.amount = Objects.requireNonNull(amount, "amount");
        if (source == null || source.isBlank()) {
            throw new DomainValidationException("INCOME_SOURCE_REQUIRED", "Income source is required");
        }
        this.source = source;
        this.active = active;
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom");
    }

    public Money amount() {
        return amount;
    }

    public String source() {
        return source;
    }

    public boolean active() {
        return active;
    }

    public LocalDate effectiveFrom() {
        return effectiveFrom;
    }

    public boolean effectiveOn(LocalDate asOf) {
        return !effectiveFrom.isAfter(asOf);
    }
}
