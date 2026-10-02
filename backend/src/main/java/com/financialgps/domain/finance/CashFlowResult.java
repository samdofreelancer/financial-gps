package com.financialgps.domain.finance;

import com.financialgps.domain.model.Money;

import java.util.Objects;

/**
 * Canonical cash-flow result. Net Cash Flow may be negative (reported);
 * {@code Mandatory Payment} is the sum of ACTIVE debts' minimumPayment (002) and is exposed as its
 * own total because the Financial Position is defined as Income − Expense − Mandatory Payment
 * (spec §8.2);
 * Available Capacity = max(NetCashFlow, 0).
 */
public record CashFlowResult(Money income, Money expense, Money mandatoryPayment,
                             Money netCashFlow, Money availableCapacity) {

    public CashFlowResult {
        Objects.requireNonNull(income, "income");
        Objects.requireNonNull(expense, "expense");
        Objects.requireNonNull(mandatoryPayment, "mandatoryPayment");
        Objects.requireNonNull(netCashFlow, "netCashFlow");
        Objects.requireNonNull(availableCapacity, "availableCapacity");
    }
}
