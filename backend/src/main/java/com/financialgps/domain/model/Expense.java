package com.financialgps.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/** Monthly money outflow with amount + category + fixed/variable type. */
public final class Expense {

    public enum ExpenseType {
        FIXED, VARIABLE
    }

    private final Money amount;
    private final String category;
    private final ExpenseType expenseType;
    private final boolean active;
    private final LocalDate effectiveFrom;

    public Expense(Money amount, String category, ExpenseType expenseType, boolean active, LocalDate effectiveFrom) {
        this.amount = Objects.requireNonNull(amount, "amount");
        if (category == null || category.isBlank()) {
            throw new DomainValidationException("EXPENSE_CATEGORY_REQUIRED", "Expense category is required");
        }
        this.category = category;
        this.expenseType = Objects.requireNonNull(expenseType, "expenseType");
        this.active = active;
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom");
    }

    public Money amount() {
        return amount;
    }

    public String category() {
        return category;
    }

    public ExpenseType expenseType() {
        return expenseType;
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
