package com.financialgps.domain;

import com.financialgps.domain.engine.Assumptions;
import com.financialgps.domain.engine.FinancialEngine;
import com.financialgps.domain.engine.FinancialResult;
import com.financialgps.domain.engine.Provenance;
import com.financialgps.domain.finance.CashFlowCalculator;
import com.financialgps.domain.finance.CashFlowResult;
import com.financialgps.domain.model.Expense;
import com.financialgps.domain.model.FinancialInput;
import com.financialgps.domain.model.Income;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.policy.FinancialPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 001 TDD RED: canonical engine path + provenance + determinism (engine-contract, DM-001..003).
 * There is exactly one cash-flow implementation; this test pins the path
 * FinancialEngine -> CashFlowCalculator -> FinancialResult.
 */
class FinancialEngineTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 16);
    private static final FinancialPolicy POLICY = FinancialPolicy.defaults();

    private static FinancialInput scenario() {
        return new FinancialInput(
                List.of(new Income(Money.of("74.00", "VND"), "salary", true, AS_OF)),
                List.of(new Expense(Money.of("30.00", "VND"), "rent", Expense.ExpenseType.FIXED, true, AS_OF)),
                List.of(), List.of());
    }

    @Test
    void engineResultMatchesTheCanonicalCashFlowCalculatorExactly() {
        FinancialResult result = FinancialEngine.calculate(scenario(), Assumptions.none(), AS_OF, POLICY);
        CashFlowResult canonical = CashFlowCalculator.calculate(scenario(), AS_OF, POLICY);

        assertThat(result.position()).isEqualTo(canonical);
    }

    @Test
    void provenanceLabelsEveryTotalCalculated() {
        FinancialResult result = FinancialEngine.calculate(scenario(), Assumptions.none(), AS_OF, POLICY);

        assertThat(result.provenance()).extracting(Provenance::field)
                .containsExactlyInAnyOrder("Income", "Expense", "Net Cash Flow", "Available Capacity");
        assertThat(result.provenance()).allSatisfy(entry -> {
            assertThat(entry.kind()).isEqualTo("calculated");
            assertThat(entry.detail()).as("every total explains its source values").isNotBlank();
        });
    }

    @Test
    void dm001_sameInputsSameAsOf_recalsIsIdentical() {
        FinancialResult first = FinancialEngine.calculate(scenario(), Assumptions.none(), AS_OF, POLICY);
        FinancialResult second = FinancialEngine.calculate(scenario(), Assumptions.none(), AS_OF, POLICY);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void dm002_dm003_asOfShiftIsExplainedAndNeverCached() {
        LocalDate futureIncomeFrom = AS_OF.plusDays(1);
        FinancialInput input = new FinancialInput(
                List.of(new Income(Money.of("74.00", "VND"), "salary", true, futureIncomeFrom)),
                List.of(new Expense(Money.of("30.00", "VND"), "rent", Expense.ExpenseType.FIXED, true, AS_OF)),
                List.of(), List.of());

        FinancialResult today = FinancialEngine.calculate(input, Assumptions.none(), AS_OF, POLICY);
        FinancialResult tomorrow = FinancialEngine.calculate(input, Assumptions.none(), futureIncomeFrom, POLICY);

        assertThat(today.position().income().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tomorrow.position().income().amount()).isEqualByComparingTo(new BigDecimal("74.00"));
        assertThat(today.asOfDate()).isEqualTo(AS_OF);
        assertThat(tomorrow.asOfDate()).isEqualTo(futureIncomeFrom);
        assertThat(today.explanations()).as("the evaluated date is always stated")
                .anyMatch(text -> text.contains(AS_OF.toString()));
        assertThat(tomorrow.explanations()).anyMatch(text -> text.contains(futureIncomeFrom.toString()));
    }

    @Test
    void cf003_negativeNetCashFlowSurvivesTheEnginePath() {
        FinancialInput input = new FinancialInput(
                List.of(new Income(Money.of("20.00", "VND"), "salary", true, AS_OF)),
                List.of(new Expense(Money.of("30.00", "VND"), "rent", Expense.ExpenseType.FIXED, true, AS_OF)),
                List.of(), List.of());

        FinancialResult result = FinancialEngine.calculate(input, Assumptions.none(), AS_OF, POLICY);

        assertThat(result.position().netCashFlow().asDecimalString()).isEqualTo("-10.00");
        assertThat(result.position().availableCapacity().asDecimalString()).isEqualTo("0.00");
    }

    @Test
    void mandatoryPaymentStaysZeroBecauseDebtLogicBelongsTo002() {
        FinancialResult result = FinancialEngine.calculate(scenario(), Assumptions.none(), AS_OF, POLICY);

        assertThat(result.provenance()).anySatisfy(entry -> {
            assertThat(entry.field()).isEqualTo("Net Cash Flow");
            assertThat(entry.detail()).contains("Mandatory Payment").contains("002");
        });
        assertThat(result.position().netCashFlow().asDecimalString()).isEqualTo("44.00");
    }
}
