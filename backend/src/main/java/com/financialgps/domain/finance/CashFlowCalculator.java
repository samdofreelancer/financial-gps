package com.financialgps.domain.finance;

import com.financialgps.domain.model.FinancialInput;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.policy.FinancialPolicy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Canonical cash-flow calculator (engine-contract internal API).
 * Income = sum(active incomes effective on asOf);
 * Expense = sum(active expenses effective on asOf);
 * Mandatory Payment = 0 for 001 (002 owns debt);
 * Net Cash Flow = Income − Expense − Mandatory (may be negative, reported);
 * Available Capacity = max(NetCashFlow, 0).
 */
public final class CashFlowCalculator {

    private CashFlowCalculator() {
    }

    public static CashFlowResult calculate(FinancialInput input, LocalDate asOf, FinancialPolicy policy) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(policy, "policy");

        String currency = currencyOf(input);
        Money income = Money.zero(currency);
        for (var in : input.incomes()) {
            if (in.active() && in.effectiveOn(asOf)) {
                income = income.add(in.amount());
            }
        }
        Money expense = Money.zero(currency);
        for (var ex : input.expenses()) {
            if (ex.active() && ex.effectiveOn(asOf)) {
                expense = expense.add(ex.amount());
            }
        }
        // Mandatory Payment = 0 for 001. Single currency enforced by Money.add.
        Money netCashFlow = income.subtract(expense);
        Money availableCapacity = netCashFlow.maxZero();
        return new CashFlowResult(income, expense, netCashFlow, availableCapacity);
    }

    public static CashFlowResult calculate(FinancialInput input, FinancialPolicy policy) {
        return calculate(input, LocalDate.now(), policy);
    }

    private static String currencyOf(FinancialInput input) {
        for (var in : input.incomes()) {
            return in.amount().currency();
        }
        for (var ex : input.expenses()) {
            return ex.amount().currency();
        }
        return "VND";
    }
}
