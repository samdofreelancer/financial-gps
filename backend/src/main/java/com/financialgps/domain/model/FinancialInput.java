package com.financialgps.domain.model;

import com.financialgps.domain.debt.Debt;

import java.util.List;
import java.util.Objects;

/**
 * Owner-free aggregate passed to calculate(...). Debts are domain Debts (002 owns them);
 * goals stay untyped until 003 owns them.
 */
public final class FinancialInput {

    private final List<Income> incomes;
    private final List<Expense> expenses;
    private final List<Debt> debts;
    private final List<Object> goals;

    public FinancialInput(List<Income> incomes, List<Expense> expenses, List<Debt> debts, List<Object> goals) {
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

    public List<Debt> debts() {
        return debts;
    }

    public List<Object> goals() {
        return goals;
    }
}
