package com.financialgps.domain.debt;

import com.financialgps.domain.finance.CashFlowCalculator;
import com.financialgps.domain.finance.CashFlowResult;
import com.financialgps.domain.model.Expense;
import com.financialgps.domain.model.Portfolio;
import com.financialgps.domain.model.Income;
import com.financialgps.domain.model.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** T003 RED: portfolio + DTI oracle Table 11.2 REF-P01–REF-P04 (spec §5.4 + §6). */
class DebtSummaryCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);
    private static final DebtCalculationPolicy POLICY = DebtCalculationPolicy.defaults();

    private static Debt debt(String creditor, String balance, String rate, String min, String plan) {
        return new Debt(creditor, DebtType.PERSONAL_LOAN, Money.of(balance, "VND"),
                Money.of(balance, "VND"), Rate.of(rate), Money.of(min, "VND"),
                Money.of(plan, "VND"), 15, DebtStatus.ACTIVE);
    }

    @Test
    void refP01_twoDebtsAggregate() {
        var debts = List.of(debt("A", "1000.00", "0.120000", "50.00", "100.00"),
                debt("B", "2000.00", "0.000000", "200.00", "200.00"));
        var r = DebtSummaryCalculator.summarize(debts, new BigDecimal("10000.00"), "VND", AS_OF, POLICY);
        assertThat(r.totalOutstandingDebt()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(r.totalMinimumMonthlyPayment()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(r.totalPlannedMonthlyPayment()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(r.debtToIncome().status()).isEqualTo("AVAILABLE");
        assertThat(r.debtToIncome().ratio()).isEqualByComparingTo(new BigDecimal("0.0250"));
        assertThat(r.portfolioProjection().status()).isEqualTo(ProjectionStatus.AVAILABLE);
        assertThat(r.portfolioProjection().projectedDebtFreeDate()).isEqualTo(LocalDate.of(2027, 9, 1));
        assertThat(r.blockedDebtCount()).isZero();
    }

    @Test
    void refP02_blockedDebtPropagatesToPortfolio() {
        var debts = List.of(debt("A", "5000.00", "0.120000", "40.00", "40.00"),
                debt("B", "1000.00", "0.000000", "100.00", "100.00"));
        var r = DebtSummaryCalculator.summarize(debts, new BigDecimal("5000.00"), "VND", AS_OF, POLICY);
        assertThat(r.portfolioProjection().status()).isEqualTo(ProjectionStatus.BLOCKED);
        assertThat(r.portfolioProjection().projectedDebtFreeDate()).isNull();
        assertThat(r.portfolioProjection().totalMonthsRemaining()).isNull();
        assertThat(r.portfolioProjection().totalInterestRemaining()).isNull();
        assertThat(r.portfolioProjection().reasonCode()).isEqualTo("PORTFOLIO_CONTAINS_BLOCKED_DEBTS");
        assertThat(r.blockedDebtCount()).isEqualTo(1);
        assertThat(r.debtToIncome().ratio()).isEqualByComparingTo(new BigDecimal("0.0280"));
    }

    @Test
    void refP03_emptyPortfolioIsCompleted() {
        var r = DebtSummaryCalculator.summarize(List.of(), new BigDecimal("20000.00"), "VND", AS_OF, POLICY);
        assertThat(r.totalOutstandingDebt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.debtToIncome().ratio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.debtToIncome().status()).isEqualTo("AVAILABLE");
        assertThat(r.portfolioProjection().status()).isEqualTo(ProjectionStatus.COMPLETED);
        assertThat(r.portfolioProjection().projectedDebtFreeDate()).isEqualTo(AS_OF);
    }

    @Test
    void refP04_zeroIncomeMakesDtiUnavailable() {
        var debts = List.of(debt("A", "1000.00", "0.000000", "100.00", "100.00"));
        var r = DebtSummaryCalculator.summarize(debts, BigDecimal.ZERO, "VND", AS_OF, POLICY);
        assertThat(r.debtToIncome().status()).isEqualTo("UNAVAILABLE");
        assertThat(r.debtToIncome().ratio()).isNull();
        assertThat(r.debtToIncome().reasonCode()).isEqualTo("ZERO_OR_MISSING_INCOME");
        assertThat(r.portfolioProjection().status()).isEqualTo(ProjectionStatus.AVAILABLE);
        assertThat(r.portfolioProjection().projectedDebtFreeDate()).isEqualTo(LocalDate.of(2027, 8, 1));
    }

    @Test
    void archivedDebtsAreExcludedFromTotals() {
        var active = debt("A", "1000.00", "0.000000", "100.00", "100.00");
        var r = DebtSummaryCalculator.summarize(List.of(active, active.archived()),
                new BigDecimal("10000.00"), "VND", AS_OF, POLICY);
        assertThat(r.totalOutstandingDebt()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void t004_mandatoryPaymentEqualsActiveMinimums_sc51() {
        var debts = List.of(debt("A", "12000000.00", "0.000000", "12000000.00", "12000000.00"),
                debt("B", "8000000.00", "0.000000", "8000000.00", "8000000.00"));
        var input = new Portfolio(
                List.of(new Income(Money.of("74000000.00", "VND"), "salary", true, AS_OF)),
                List.of(new Expense(Money.of("30000000.00", "VND"), "living",
                        Expense.ExpenseType.FIXED, true, AS_OF)),
                debts, List.of());
        CashFlowResult r = CashFlowCalculator.calculate(input, AS_OF,
                com.financialgps.domain.policy.FinancialPolicy.defaults());
        assertThat(r.mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(r.netCashFlow().asDecimalString()).isEqualTo("24000000.00");
        assertThat(r.availableCapacity().asDecimalString()).isEqualTo("24000000.00");
    }

    @Test
    void t004_negativeNetCashFlowIsReported_sc52() {
        var debts = List.of(debt("A", "15000000.00", "0.000000", "15000000.00", "15000000.00"));
        var input = new Portfolio(
                List.of(new Income(Money.of("30000000.00", "VND"), "salary", true, AS_OF)),
                List.of(new Expense(Money.of("20000000.00", "VND"), "living",
                        Expense.ExpenseType.FIXED, true, AS_OF)),
                debts, List.of());
        CashFlowResult r = CashFlowCalculator.calculate(input, AS_OF,
                com.financialgps.domain.policy.FinancialPolicy.defaults());
        assertThat(r.mandatoryPayment().asDecimalString()).isEqualTo("15000000.00");
        assertThat(r.netCashFlow().asDecimalString()).isEqualTo("-5000000.00");
        assertThat(r.availableCapacity().asDecimalString()).isEqualTo("0.00");
    }
}
