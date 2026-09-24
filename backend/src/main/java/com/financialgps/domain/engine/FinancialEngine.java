package com.financialgps.domain.engine;

import com.financialgps.domain.finance.CashFlowCalculator;
import com.financialgps.domain.finance.CashFlowResult;
import com.financialgps.domain.model.FinancialInput;
import com.financialgps.domain.policy.FinancialPolicy;

import java.time.LocalDate;
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
            FinancialInput input,
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
                provenance(asOfDate),
                explanations(asOfDate));
    }

    /** Every total states how it was derived (US2 / FR-003: facts vs calculated totals). */
    private static List<Provenance> provenance(LocalDate asOfDate) {
        return List.of(
                new Provenance("Income", "calculated",
                        "sum of active incomes effective on " + asOfDate),
                new Provenance("Expense", "calculated",
                        "sum of active expenses effective on " + asOfDate),
                new Provenance("Net Cash Flow", "calculated",
                        "Income - Expense - Mandatory Payment (0.00; debt logic belongs to 002)"),
                new Provenance("Available Capacity", "calculated",
                        "max(Net Cash Flow, 0.00)"));
    }

    /**
     * The evaluated date is always stated, so an as-of shift is explained rather than served from a
     * stale cache (DM-002/DM-003).
     */
    private static List<String> explanations(LocalDate asOfDate) {
        return List.of(
                "MONTHLY periods evaluated as of " + asOfDate,
                "Mandatory Payment is 0.00 for this feature; debt logic belongs to 002");
    }
}
