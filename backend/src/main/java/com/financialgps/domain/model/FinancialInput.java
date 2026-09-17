package com.financialgps.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Owner-free aggregate passed to calculate(...). For 001 only incomes/expenses
 * are populated; debts/goals lists stay empty (002/003 own them).
 */
public final class FinancialInput {

    private final List<Income> incomes;
    private final List<Expense> expenses;
    private final List<Object> debts;
    private final List<Object> goals;

    public FinancialInput(List<Income> incomes, List<Expense> expenses, List<Object> debts, List<Object> goals) {
        this.incomes = List.copyOf(Objects.requireNonNull(incomes, "incomes"));
        this.expenses = List.copyOf(Objects.requireNonNull(expenses, "expenses"));
        this.debts = List.copyOf(Objects.requireNonNull(debts, "debts"));
        this.goals = List.copyOf(Objects.requireNonNull(goals, "goals"));
    }

    public static FinancialInput empty() {
        return new FinancialInput(List.of(), List.of(), List.of(), List.of());
    }

    public List<Income> incomes() {
        return incomes;
    }

    public List<Expense> expenses() {
        return expenses;
    }

    public List<Object> debts() {
        return debts;
    }

    public List<Object> goals() {
        return goals;
    }
}
