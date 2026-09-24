package com.financialgps.domain.finance;

import com.financialgps.domain.model.Money;

import java.util.Objects;

/**
 * Canonical cash-flow result. Net Cash Flow may be negative (reported);
 * Available Capacity = max(NetCashFlow, 0).
 */
public record CashFlowResult(Money income, Money expense, Money netCashFlow, Money availableCapacity) {

    public CashFlowResult {
        Objects.requireNonNull(income, "income");
        Objects.requireNonNull(expense, "expense");
        Objects.requireNonNull(netCashFlow, "netCashFlow");
        Objects.requireNonNull(availableCapacity, "availableCapacity");
    }
}
