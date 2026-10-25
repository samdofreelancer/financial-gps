package com.financialgps.domain.goal;

import com.financialgps.domain.model.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T003: oracle Tables 9.2 REF-C01–REF-C06 and 9.3 REF-A01–REF-A06 (spec §4.3, §9). */
class GoalCapacityCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);

    private static Goal goal(String target, String current, LocalDate targetDate) {
        return Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND", target, current,
                targetDate, 1);
    }

    private static Money vnd(String amount) {
        return Money.of(amount, "VND");
    }

    private static Goal remainingGoal(String remaining, LocalDate targetDate) {
        // current 0, target == remaining → remaining == target.
        return goal(remaining, "0.00", targetDate);
    }

    @Test
    void refC01_exactDivisor() {
        var c = GoalCapacityCalculator.evaluate(
                remainingGoal("120000000.00", LocalDate.of(2027, 10, 1)), vnd("24000000.00"), AS_OF);
        assertThat(c.monthsRemaining()).isEqualTo(12);
        assertThat(c.requiredMonthlyCapacity().asDecimalString()).isEqualTo("10000000.00");
    }

    @Test
    void refC02_ceilingNeverUnderstated() {
        var c = GoalCapacityCalculator.evaluate(
                remainingGoal("121000000.00", LocalDate.of(2027, 10, 1)), vnd("24000000.00"), AS_OF);
        assertThat(c.monthsRemaining()).isEqualTo(12);
        assertThat(c.requiredMonthlyCapacity().asDecimalString()).isEqualTo("10083333.34");
    }

    @Test
    void refC03_undatedGoal() {
        var c = GoalCapacityCalculator.evaluate(
                remainingGoal("90000000.00", null), vnd("24000000.00"), AS_OF);
        assertThat(c.monthsRemaining()).isNull();
        assertThat(c.requiredMonthlyCapacity()).isNull();
        assertThat(c.capacityCoverage())
                .isEqualTo(GoalCapacity.CapacityCoverage.NOT_APPLICABLE);
        assertThat(c.dateFeasibility()).isEqualTo(GoalCapacity.DateFeasibility.UNDATED);
    }

    @Test
    void refC04_targetTodayIsExpired() {
        var c = GoalCapacityCalculator.evaluate(
                remainingGoal("5000000.00", AS_OF), vnd("24000000.00"), AS_OF);
        assertThat(c.monthsRemaining()).isZero();
        assertThat(c.requiredMonthlyCapacity().asDecimalString()).isEqualTo("5000000.00");
        assertThat(c.dateFeasibility())
                .isEqualTo(GoalCapacity.DateFeasibility.EXPIRED_TARGET_DATE);
    }

    @Test
    void refC05_targetInThePast() {
        var c = GoalCapacityCalculator.evaluate(
                remainingGoal("5000000.00", LocalDate.of(2025, 12, 31)), vnd("24000000.00"), AS_OF);
        assertThat(c.monthsRemaining()).isZero();
        assertThat(c.requiredMonthlyCapacity().asDecimalString()).isEqualTo("5000000.00");
        assertThat(c.dateFeasibility())
                .isEqualTo(GoalCapacity.DateFeasibility.EXPIRED_TARGET_DATE);
    }

    @Test
    void refC06_completedGoalNeedsNothing() {
        var c = GoalCapacityCalculator.evaluate(
                goal("120000000.00", "120000000.00", LocalDate.of(2027, 1, 1)),
                vnd("0.00"), AS_OF);
        assertThat(c.requiredMonthlyCapacity().asDecimalString()).isEqualTo("0.00");
        assertThat(c.capacityCoverage())
                .isEqualTo(GoalCapacity.CapacityCoverage.NOT_APPLICABLE);
        assertThat(c.dateFeasibility()).isEqualTo(GoalCapacity.DateFeasibility.COMPLETED);
    }

    @Test
    void monthsRemaining_largestMFittingBeforeTarget() {
        // 2026-10-01 + 12 months = 2027-10-01 <= target → 12; +13 exceeds → stays 12.
        assertThat(GoalCapacityCalculator.monthsBetween(AS_OF, LocalDate.of(2027, 10, 1)))
                .isEqualTo(12);
        assertThat(GoalCapacityCalculator.monthsBetween(AS_OF, LocalDate.of(2027, 10, 15)))
                .isEqualTo(12);
        assertThat(GoalCapacityCalculator.monthsBetween(AS_OF, LocalDate.of(2026, 11, 1)))
                .isEqualTo(1);
        assertThat(GoalCapacityCalculator.monthsBetween(AS_OF, AS_OF)).isZero();
    }

    private static GoalCapacity coverage(String requiredRemaining, String available) {
        return GoalCapacityCalculator.evaluate(
                remainingGoal(requiredRemaining, LocalDate.of(2027, 10, 1)), vnd(available), AS_OF);
    }

    @Test
    void refA01_surplusIsMeets() {
        var c = coverage("120000000.00", "24000000.00");
        assertThat(c.capacityCoverage()).isEqualTo(GoalCapacity.CapacityCoverage.MEETS_REQUIRED);
        assertThat(c.monthlyShortfall().asDecimalString()).isEqualTo("0.00");
    }

    @Test
    void refA02_equalityIsSufficient() {
        var c = coverage("120000000.00", "10000000.00");
        assertThat(c.capacityCoverage()).isEqualTo(GoalCapacity.CapacityCoverage.MEETS_REQUIRED);
        assertThat(c.monthlyShortfall().asDecimalString()).isEqualTo("0.00");
    }

    @Test
    void refA03_shortfallStatesExactGap() {
        var c = coverage("120000000.00", "9000000.00");
        assertThat(c.capacityCoverage()).isEqualTo(GoalCapacity.CapacityCoverage.SHORTFALL);
        assertThat(c.monthlyShortfall().asDecimalString()).isEqualTo("1000000.00");
    }

    @Test
    void refA04_zeroCapacityFullShortfall() {
        var c = coverage("120000000.00", "0.00");
        assertThat(c.capacityCoverage()).isEqualTo(GoalCapacity.CapacityCoverage.SHORTFALL);
        assertThat(c.monthlyShortfall().asDecimalString()).isEqualTo("10000000.00");
    }

    @Test
    void refA05_undatedNotApplicable() {
        var c = GoalCapacityCalculator.evaluate(
                remainingGoal("90000000.00", null), vnd("24000000.00"), AS_OF);
        assertThat(c.capacityCoverage())
                .isEqualTo(GoalCapacity.CapacityCoverage.NOT_APPLICABLE);
        assertThat(c.monthlyShortfall()).isNull();
    }

    @Test
    void determinism_sameInputsSameAsOf_identicalResult() {
        Goal g = remainingGoal("120000000.00", LocalDate.of(2027, 10, 1));
        assertThat(GoalCapacityCalculator.evaluate(g, vnd("9000000.00"), AS_OF))
                .isEqualTo(GoalCapacityCalculator.evaluate(g, vnd("9000000.00"), AS_OF));
    }

    @Test
    void mixedCurrenciesAreRejectedNeverSilentlyComputed() {
        Goal g = remainingGoal("120000000.00", LocalDate.of(2027, 10, 1));
        assertThatThrownBy(() -> GoalCapacityCalculator.evaluate(g,
                        com.financialgps.domain.model.Money.of("9000000.00", "USD"), AS_OF))
                .isInstanceOf(com.financialgps.domain.model.DomainValidationException.class)
                .hasMessageContaining("currency");
        assertThatThrownBy(() -> GoalCapacityCalculator.evaluate(g, "9000000.00", "USD", AS_OF,
                        GoalCalculationPolicy.defaults()))
                .isInstanceOf(com.financialgps.domain.model.DomainValidationException.class);
    }

    @Test
    void corruptAvailableAmountIsReportedNeverZeroed() {
        Goal g = remainingGoal("120000000.00", LocalDate.of(2027, 10, 1));
        assertThatThrownBy(() -> GoalCapacityCalculator.evaluate(g, "not-a-number", "VND", AS_OF,
                        GoalCalculationPolicy.defaults()))
                .isInstanceOf(com.financialgps.domain.model.DomainValidationException.class);
        assertThatThrownBy(() -> GoalCapacityCalculator.evaluate(g, null, "VND", AS_OF,
                        GoalCalculationPolicy.defaults()))
                .isInstanceOf(com.financialgps.domain.model.DomainValidationException.class);
    }
}
