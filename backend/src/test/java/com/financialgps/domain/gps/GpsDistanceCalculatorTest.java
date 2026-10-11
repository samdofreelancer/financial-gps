package com.financialgps.domain.gps;

import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.debt.ProjectionStatus;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 004: distance follows the Feature 003 rules (never negative, zero-target safe)
 * and reads debt-freedom distance from the Feature 002 portfolio.
 */
class GpsDistanceCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 11);

    private static DebtSummaryResult debts(String outstanding) {
        return new DebtSummaryResult(
                new BigDecimal(outstanding), new BigDecimal("20000000.00"),
                new BigDecimal("20000000.00"), null, "VND",
                new DebtSummaryResult.DtiResult("AVAILABLE", new BigDecimal("0.2703"), null, "dti"),
                new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.AVAILABLE,
                        AS_OF.plusMonths(7), 7, new BigDecimal("1000000.00"), null, "payoff", List.of()),
                0, AS_OF);
    }

    @Test
    void amountDistance_isRemainingNeverNegative() {
        var goal = Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND",
                "108000000.00", "20000000.00", LocalDate.of(2027, 3, 1), 1);
        var distance = (GpsDistance.AmountBased) GpsDistanceCalculator.calculate(
                goal, debts("120000000.00"), AS_OF, GoalCalculationPolicy.defaults());
        assertThat(distance.remaining().asDecimalString()).isEqualTo("88000000.00");
        assertThat(distance.progressPercent()).isEqualByComparingTo(new BigDecimal("0.1852"));
    }

    @Test
    void overfundedGoal_clampsToZeroDistanceAndFullProgress() {
        var goal = Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND",
                "108000000.00", "150000000.00", LocalDate.of(2027, 3, 1), 1);
        var distance = (GpsDistance.AmountBased) GpsDistanceCalculator.calculate(
                goal, debts("120000000.00"), AS_OF, GoalCalculationPolicy.defaults());
        assertThat(distance.remaining().asDecimalString()).isEqualTo("0.00");
        assertThat(distance.progressPercent()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(distance.isCompleted()).isTrue();
    }

    @Test
    void zeroTarget_doesNotThrowAndReportsCompleted() {
        var goal = Goal.recorded("VND", "Tiny", "EMERGENCY_FUND",
                "0.00", "0.00", null, 1);
        // Would throw ArithmeticException before the 003-rules fix; a throw fails the test.
        var distance = (GpsDistance.AmountBased) GpsDistanceCalculator.calculate(
                goal, debts("0.00"), AS_OF, GoalCalculationPolicy.defaults());
        assertThat(distance.remaining().asDecimalString()).isEqualTo("0.00");
        assertThat(distance.progressPercent()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void debtFreedom_distanceIsPortfolioOutstanding() {
        var goal = Goal.recorded("VND", "Debt Freedom", "DEBT_FREEDOM", null, null,
                LocalDate.of(2027, 12, 31), 1);
        var distance = (GpsDistance.DebtFreedom) GpsDistanceCalculator.calculate(
                goal, debts("120000000.00"), AS_OF, GoalCalculationPolicy.defaults());
        assertThat(distance.totalOutstandingDebt().asDecimalString()).isEqualTo("120000000.00");
    }
}
