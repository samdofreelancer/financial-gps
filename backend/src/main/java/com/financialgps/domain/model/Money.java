package com.financialgps.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pure domain Money: decimal amount (scale 2) + ISO currency.
 * No Spring/Jakarta/SQL. Negative amounts are rejected at construction.
 */
public final class Money {

    public static final int SCALE = 2;

    /**
     * Canonical decimal-string form (calculation-rules §1): optional sign, plain digits, optional
     * fraction. Scientific notation, locale separators and surrounding whitespace are rejected
     * instead of being silently reinterpreted — money never travels as a float.
     */
    private static final Pattern DECIMAL_STRING = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(String amount, String currency) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (!DECIMAL_STRING.matcher(amount).matches()) {
            throw new DomainValidationException("MONEY_INVALID",
                    "Monetary amounts must be plain decimal strings (for example 74.00)");
        }
        BigDecimal value = new BigDecimal(amount).setScale(SCALE, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("MONEY_NEGATIVE", "Monetary amounts must not be negative");
        }
        if (currency.isBlank()) {
            throw new DomainValidationException("CURRENCY_REQUIRED", "Currency is required");
        }
        return new Money(value, currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO.setScale(SCALE), currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /** Available Capacity clamp: max(value, 0) — never conceals the Net Cash Flow itself. */
    public Money maxZero() {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return zero(currency);
        }
        return this;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    /** Decimal-string form for JSON (never a JSON number). */
    public String asDecimalString() {
        return amount.toPlainString();
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new DomainValidationException("CURRENCY_MISMATCH", "Single-currency v1: currency conversion is out of scope");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        return amount.compareTo(other.amount) == 0 && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
