package com.financialgps.domain.finance;

import com.financialgps.domain.model.Money;
import com.financialgps.domain.model.Obligation;
import com.financialgps.domain.model.Portfolio;
import com.financialgps.domain.policy.FinancialPolicy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Canonical cash-flow calculator (engine-contract internal API).
 * Income = sum(active incomes effective on asOf);
 * Expense = sum(active expenses effective on asOf);
 * Mandatory Payment = sum(obligations);
 * Net Cash Flow = Income − Expense − Mandatory (may be negative, reported);
 * Available Capacity = max(NetCashFlow, 0).
 *
 * <p>The evaluation date is always explicit: there is no clock-reading overload, so tests and
 * callers cannot accidentally evaluate "now" (constitution: deterministic calculations).
 */
public final class CashFlowCalculator {

    private CashFlowCalculator() {
    }

    public static CashFlowResult calculate(Portfolio input, LocalDate asOf, FinancialPolicy policy) {
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
        // Mandatory Payment = sum of obligations (002 maps ACTIVE debts at the port boundary).
        // Conservation: every unit of committed cash reduces Net Cash Flow one-for-one.
        Money mandatory = Money.zero(currency);
        for (Obligation obligation : input.obligations()) {
            mandatory = mandatory.add(obligation.amount());
        }
        Money netCashFlow = income.subtract(expense).subtract(mandatory);
        Money availableCapacity = netCashFlow.maxZero();
        return new CashFlowResult(income, expense, mandatory, netCashFlow, availableCapacity);
    }

    private static String currencyOf(Portfolio input) {
        for (var in : input.incomes()) {
            return in.amount().currency();
        }
        for (var ex : input.expenses()) {
            return ex.amount().currency();
        }
        return Money.DEFAULT_CURRENCY;
    }
}
