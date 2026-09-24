package com.financialgps.domain;

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
 * 001 TDD RED: cash-flow oracle CF-001..003 + 001 edge cases.
 * Canonical path only: CashFlowCalculator (called by FinancialEngine).
 */
class CashFlowCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 16);
    private static final FinancialPolicy POLICY = FinancialPolicy.defaults();

    private static Income income(String amount, String source, boolean active) {
        return new Income(Money.of(amount, "VND"), source, active, AS_OF);
    }

    private static Expense expense(String amount, String category, boolean active) {
        return new Expense(Money.of(amount, "VND"), category, Expense.ExpenseType.VARIABLE, active, AS_OF);
    }

    @Test
    void cf001_income74_expense30_mandatory0_yieldsNcf24_capacity24() {
        FinancialInput input = new FinancialInput(
                List.of(income("74.00", "salary", true)),
                List.of(expense("30.00", "rent", true)),
                List.of(), List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.income().amount()).isEqualByComparingTo(new BigDecimal("74.00"));
        assertThat(result.expense().amount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.netCashFlow().amount()).isEqualByComparingTo(new BigDecimal("44.00"));
        assertThat(result.availableCapacity().amount()).isEqualByComparingTo(new BigDecimal("44.00"));
    }

    @Test
    void cf002_equalIncomeExpense_yieldsZeroNcfAndZeroCapacity() {
        FinancialInput input = new FinancialInput(
                List.of(income("30.00", "salary", true)),
                List.of(expense("30.00", "rent", true)),
                List.of(), List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.netCashFlow().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.availableCapacity().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cf003_negativeNcfIsReported_capacityClampsToZero() {
        FinancialInput input = new FinancialInput(
                List.of(income("20.00", "salary", true)),
                List.of(expense("30.00", "rent", true)),
                List.of(), List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        // Negative Net Cash Flow must be reported, never concealed.
        assertThat(result.netCashFlow().amount()).isEqualByComparingTo(new BigDecimal("-10.00"));
        assertThat(result.availableCapacity().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void emptyProfile_yieldsZeros() {
        FinancialInput input = FinancialInput.empty();

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.income().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.expense().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netCashFlow().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.availableCapacity().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void inactiveLinesAreExcluded() {
        FinancialInput input = new FinancialInput(
                List.of(income("100.00", "old job", false), income("50.00", "salary", true)),
                List.of(expense("999.00", "old rent", false), expense("20.00", "rent", true)),
                List.of(), List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.income().amount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.expense().amount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.netCashFlow().amount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void multipleLinesSum_exactDecimalPrecision() {
        FinancialInput input = new FinancialInput(
                List.of(income("19.99", "a", true), income("0.01", "b", true)),
                List.of(expense("0.10", "x", true), expense("0.20", "y", true)),
                List.of(), List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.income().amount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.expense().amount()).isEqualByComparingTo(new BigDecimal("0.30"));
        assertThat(result.netCashFlow().amount()).isEqualByComparingTo(new BigDecimal("19.70"));
    }

    @Test
    void determinism_sameInputTwice_identicalResult() {
        FinancialInput input = new FinancialInput(
                List.of(income("74.00", "salary", true)),
                List.of(expense("30.00", "rent", true)),
                List.of(), List.of());

        CashFlowResult first = CashFlowCalculator.calculate(input, AS_OF, POLICY);
        CashFlowResult second = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(second).isEqualTo(first);
    }
}
