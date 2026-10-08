package com.financialgps.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Thin owner-free portfolio aggregate passed to the engine (incomes, expenses, obligations,
 * goals). Obligations are shared-kernel {@link Obligation}s (002 maps its debts at the port
 * boundary); goals stay untyped until a later feature owns them.
 */
public final class Portfolio {

    private final List<Income> incomes;
    private final List<Expense> expenses;
    private final List<Obligation> obligations;
    private final List<Object> goals;

    public Portfolio(List<Income> incomes, List<Expense> expenses, List<Obligation> obligations,
                     List<Object> goals) {
        this.incomes = List.copyOf(Objects.requireNonNull(incomes, "incomes"));
        this.expenses = List.copyOf(Objects.requireNonNull(expenses, "expenses"));
        this.obligations = List.copyOf(Objects.requireNonNull(obligations, "obligations"));
        this.goals = List.copyOf(Objects.requireNonNull(goals, "goals"));
    }

    public static Portfolio empty() {
        return new Portfolio(List.of(), List.of(), List.of(), List.of());
    }

    public List<Income> incomes() {
        return incomes;
    }

    public List<Expense> expenses() {
        return expenses;
    }

    public List<Obligation> obligations() {
        return obligations;
    }

    public List<Object> goals() {
        return goals;
    }
}
