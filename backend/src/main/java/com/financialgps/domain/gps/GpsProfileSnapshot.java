package com.financialgps.domain.gps;

import java.util.List;
import java.util.Objects;

/**
 * Framework-free snapshot of the owner's financial profile facts needed by the GPS
 * current-position engine. The application layer maps its profile view onto this
 * record at the boundary, so the domain never receives application types
 * (the engine never receives identity).
 *
 * <p>A {@code null} currency marks a missing profile: profile-dependent money is then
 * reported {@code UNAVAILABLE}, never fabricated as zero.
 */
public record GpsProfileSnapshot(
        String currency,
        List<IncomeLine> incomes,
        List<ExpenseLine> expenses,
        String savingsAmount,
        String emergencyFundAmount,
        int dependentsCount) {

    public GpsProfileSnapshot {
        Objects.requireNonNull(incomes, "incomes");
        Objects.requireNonNull(expenses, "expenses");
        incomes = List.copyOf(incomes);
        expenses = List.copyOf(expenses);
    }

    /** One income line: decimal amount string plus its source label. */
    public record IncomeLine(String amount, String source) {
        public IncomeLine {
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(source, "source");
        }
    }

    /** One expense line: decimal amount string plus category and FIXED/VARIABLE type. */
    public record ExpenseLine(String amount, String category, String expenseType) {
        public ExpenseLine {
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(expenseType, "expenseType");
        }
    }
}
