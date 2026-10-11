package com.financialgps.domain.gps;

import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.debt.ProjectionStatus;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.policy.FinancialPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 004: current position derives Mandatory Payment from the Feature 002 debt summary
 * (single source of truth), with Net Cash Flow and Available Capacity following
 * calculation-rules (Income - Expense - Mandatory; max(Net, 0)).
 */
class CurrentPositionCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 11);

    private static ProfileModels.ProfileView profile() {
        return new ProfileModels.ProfileView(
                "VND", "20000000.00", "20000000.00", 0,
                List.of(new ProfileModels.IncomeLineView("i1", "74000000.00", "VND", "Salary", "actual")),
                List.of(new ProfileModels.ExpenseLineView("e1", "43000000.00", "VND", "Living", "FIXED", "actual")),
                null, null, null, null, null,
                List.of(), AS_OF.toString());
    }

    private static DebtSummaryResult debtSummary(String totalMin, String totalOutstanding) {
        return new DebtSummaryResult(
                new BigDecimal(totalOutstanding),
                new BigDecimal(totalMin),
                new BigDecimal(totalMin),
                null,
                "VND",
                new DebtSummaryResult.DtiResult("AVAILABLE", new BigDecimal("0.2703"), null, "dti"),
                new DebtSummaryResult.PortfolioProjectionResult(
                        ProjectionStatus.AVAILABLE, AS_OF.plusMonths(7), 7,
                        new BigDecimal("4385538.04"), null, "payoff", List.of()),
                0, AS_OF);
    }

    private static String amount(GpsCurrentPosition.MoneyValue v) {
        return ((GpsCurrentPosition.Available) v).amount().asDecimalString();
    }

    @Test
    void mandatoryComesFromDebtSummary_notZero() {
        var position = CurrentPositionCalculator.calculate(
                new CurrentPositionCalculator.Input(profile(), debtSummary("20000000.00", "120000000.00"),
                        AS_OF, FinancialPolicy.defaults()));

        assertThat(amount(position.mandatoryPayment())).isEqualTo("20000000.00");
        assertThat(amount(position.netCashFlow())).isEqualTo("11000000.00");
        assertThat(amount(position.availableCapacity())).isEqualTo("11000000.00");
        assertThat(amount(position.income())).isEqualTo("74000000.00");
        assertThat(amount(position.expense())).isEqualTo("43000000.00");
        assertThat(amount(position.totalOutstandingDebt())).isEqualTo("120000000.00");
    }

    @Test
    void negativeNetCashFlow_reportedButCapacityClampedToZero() {
        var position = CurrentPositionCalculator.calculate(
                new CurrentPositionCalculator.Input(profile(), debtSummary("40000000.00", "120000000.00"),
                        AS_OF, FinancialPolicy.defaults()));

        assertThat(amount(position.mandatoryPayment())).isEqualTo("40000000.00");
        assertThat(amount(position.netCashFlow())).isEqualTo("-9000000.00");
        assertThat(amount(position.availableCapacity())).isEqualTo("0.00");
    }

    @Test
    void missingProfile_marksPositionUnavailable() {
        var empty = new ProfileModels.ProfileView(
                null, null, null, 0, List.of(), List.of(),
                null, null, null, null, null, List.of(), null);
        var position = CurrentPositionCalculator.calculate(
                new CurrentPositionCalculator.Input(empty, debtSummary("0.00", "0.00"),
                        AS_OF, FinancialPolicy.defaults()));

        assertThat(position.income()).isInstanceOf(GpsCurrentPosition.Unavailable.class);
        assertThat(position.availableCapacity()).isInstanceOf(GpsCurrentPosition.Unavailable.class);
        // Debt totals are independently sourced and stay available.
        assertThat(position.totalOutstandingDebt()).isInstanceOf(GpsCurrentPosition.Available.class);
    }

    @Test
    void moneyScale_staysDecimal() {
        var position = CurrentPositionCalculator.calculate(
                new CurrentPositionCalculator.Input(profile(), debtSummary("20000000.00", "120000000.00"),
                        AS_OF, FinancialPolicy.defaults()));
        Money mandatory = ((GpsCurrentPosition.Available) position.mandatoryPayment()).amount();
        assertThat(mandatory.amount().scale()).isEqualTo(Money.SCALE);
    }
}
