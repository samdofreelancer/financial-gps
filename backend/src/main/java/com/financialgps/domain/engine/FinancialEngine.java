package com.financialgps.domain.engine;

import com.financialgps.domain.finance.CashFlowCalculator;
import com.financialgps.domain.finance.CashFlowResult;
import com.financialgps.domain.model.Portfolio;
import com.financialgps.domain.policy.FinancialPolicy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minimal canonical entry point for 001. Full GPS orchestration
 * (validate → timeline → cash flow → debt → deps → allocation → goal →
 * status → assemble) grows here in 002–004; 001 uses the cash-flow step
 * through this path so no second calculator can diverge.
 */
public final class FinancialEngine {

    private FinancialEngine() {
    }

    public static FinancialResult calculate(
            Portfolio input,
            Assumptions assumptions,
            LocalDate asOfDate,
            FinancialPolicy policy) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(assumptions, "assumptions");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(policy, "policy");

        CashFlowResult cashFlow = CashFlowCalculator.calculate(input, asOfDate, policy);
        return new FinancialResult(
                asOfDate,
                cashFlow,
                provenance(asOfDate, hasObligations(input)),
                explanations(asOfDate));
    }

    /** Whether the owner has any mandatory commitment (002 maps ACTIVE debts at the port). */
    private static boolean hasObligations(Portfolio input) {
        return !input.obligations().isEmpty();
    }

    /**
     * Every total states how it was derived (US2 / FR-003: facts vs calculated totals).
     *
     * <p>The 002 "Mandatory Payment" total is only announced when the owner actually has a debt: an
     * owner with none gets the 001 four-total contract exactly as before, while an owner with debt
     * gets the five-total contract that the dashboard renders (spec §8.2).
     */
    private static List<Provenance> provenance(LocalDate asOfDate, boolean hasDebts) {
        List<Provenance> entries = new ArrayList<>(List.of(
                new Provenance("Income", "calculated",
                        "sum of active incomes effective on " + asOfDate),
                new Provenance("Expense", "calculated",
                        "sum of active expenses effective on " + asOfDate)));
        if (hasDebts) {
            entries.add(new Provenance("Mandatory Payment", "calculated",
                    "sum of ACTIVE debts minimumPayment (002)"));
        }
        entries.add(new Provenance("Net Cash Flow", "calculated",
                hasDebts ? "Income - Expense - Mandatory Payment" : "Income - Expense"));
        entries.add(new Provenance("Available Capacity", "calculated",
                "max(Net Cash Flow, 0.00)"));
        return List.copyOf(entries);
    }

    /**
     * The evaluated date is always stated, so an as-of shift is explained rather than served from a
     * stale cache (DM-002/DM-003).
     */
    private static List<String> explanations(LocalDate asOfDate) {
        return List.of(
                "MONTHLY periods evaluated as of " + asOfDate,
                "Mandatory Payment is the sum of ACTIVE debts minimumPayment");
    }
}
