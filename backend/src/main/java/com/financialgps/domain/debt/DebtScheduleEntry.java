package com.financialgps.domain.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One scheduled payment: what the borrower pays on {@code dueDate}, how it splits between
 * principal and interest, and the balance that remains after it. All amounts are scale-2,
 * rounded with the same HALF_UP policy as the payoff projection (spec §5.2).
 *
 * <p>{@code dueDate} honours the debt's {@code dueDay} (clamped to the month's length) so the
 * schedule reads as the contractual calendar; when no due day is known it falls back to the
 * projection's {@code asOf + n months} rule.
 */
public record DebtScheduleEntry(
        int period,
        LocalDate dueDate,
        BigDecimal payment,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal endingBalance) {

    public DebtScheduleEntry {
        Objects.requireNonNull(dueDate, "dueDate");
        Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(interest, "interest");
        Objects.requireNonNull(endingBalance, "endingBalance");
        if (period < 1) {
            throw new IllegalArgumentException("period must be >= 1");
        }
    }
}
