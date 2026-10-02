package com.financialgps.domain.debt;

import com.financialgps.domain.model.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Annual nominal interest rate as a non-negative fraction (spec §7): {@code 0.120000} = 12%/year.
 * Scale 6, never floating point. A missing rate is represented by {@code null} on the owning
 * {@link Debt} — never by silently assuming 0% (Constitution I, spec §5.3 INTEREST_RATE_MISSING).
 */
public final class Rate {

    public static final int SCALE = 6;

    private static final Pattern DECIMAL_STRING = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private final BigDecimal value;

    private Rate(BigDecimal value) {
        this.value = value;
    }

    public static Rate of(String decimal) {
        Objects.requireNonNull(decimal, "decimal");
        if (!DECIMAL_STRING.matcher(decimal).matches()) {
            throw new DomainValidationException("RATE_INVALID",
                    "Interest rate must be a plain decimal fraction (for example 0.120000)");
        }
        BigDecimal value = new BigDecimal(decimal).setScale(SCALE, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("RATE_NEGATIVE", "Interest rate must not be negative");
        }
        return new Rate(value);
    }

    public static Rate zero() {
        return new Rate(BigDecimal.ZERO.setScale(SCALE));
    }

    public BigDecimal value() {
        return value;
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public String asDecimalString() {
        return value.toPlainString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rate other)) {
            return false;
        }
        return value.compareTo(other.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
