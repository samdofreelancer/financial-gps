package com.financialgps.domain.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** T002: oracle Table 9.1 REF-G01–REF-G08 (spec §4.2, §9). */
class GoalProgressCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);

    private static Goal goal(String target, String current, LocalDate targetDate) {
        return Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND", target, current,
                targetDate, 1);
    }

    @Test
    void refG01_standardPartialProgress() {
        var p = GoalProgressCalculator.evaluate(
                goal("120000000.00", "30000000.00", LocalDate.of(2027, 12, 31)), AS_OF);
        assertThat(p.remaining().asDecimalString()).isEqualTo("90000000.00");
        assertThat(p.progress()).isEqualByComparingTo(new BigDecimal("0.2500"));
        assertThat(p.status()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void refG02_exactCompletion() {
        var p = GoalProgressCalculator.evaluate(
                goal("120000000.00", "120000000.00", LocalDate.of(2027, 12, 31)), AS_OF);
        assertThat(p.remaining().asDecimalString()).isEqualTo("0.00");
        assertThat(p.progress()).isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(p.status()).isEqualTo(GoalStatus.COMPLETED);
    }

    @Test
    void refG03_overTargetClamps() {
        var p = GoalProgressCalculator.evaluate(
                goal("120000000.00", "150000000.00", LocalDate.of(2027, 12, 31)), AS_OF);
        assertThat(p.remaining().asDecimalString()).isEqualTo("0.00");
        assertThat(p.progress()).isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(p.status()).isEqualTo(GoalStatus.COMPLETED);
    }

    @Test
    void refG04_zeroTargetCompleteAtCreation() {
        Goal g = goal("0.00", "0.00", null);
        assertThat(g.status()).isEqualTo(GoalStatus.COMPLETED);
        var p = GoalProgressCalculator.evaluate(g, AS_OF);
        assertThat(p.remaining().asDecimalString()).isEqualTo("0.00");
        assertThat(p.progress()).isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    void refG05_zeroCurrent() {
        var p = GoalProgressCalculator.evaluate(
                goal("120000000.00", "0.00", LocalDate.of(2027, 12, 31)), AS_OF);
        assertThat(p.remaining().asDecimalString()).isEqualTo("120000000.00");
        assertThat(p.progress()).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(p.status()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void refG08_completionReevaluatedOnUpdate() {
        Goal active = goal("120000000.00", "30000000.00", LocalDate.of(2027, 12, 31));
        Goal updated = Goal.recorded("VND", active.name(), active.goalType().name(),
                "120000000.00", "120000000.00", active.targetDate(), active.priority())
                .withId(GoalId.of(java.util.UUID.randomUUID()));
        var p = GoalProgressCalculator.evaluate(updated, AS_OF);
        assertThat(p.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(p.progress()).isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    void datedVsUndated_progressIdentical() {
        var dated = GoalProgressCalculator.evaluate(
                goal("120000000.00", "30000000.00", LocalDate.of(2027, 12, 31)), AS_OF);
        var undated = GoalProgressCalculator.evaluate(
                goal("120000000.00", "30000000.00", null), AS_OF);
        assertThat(undated.remaining().asDecimalString())
                .isEqualTo(dated.remaining().asDecimalString());
        assertThat(undated.progress()).isEqualByComparingTo(dated.progress());
    }

    @Test
    void determinism_sameInputsSameAsOf_identicalResult() {
        Goal g = goal("120000000.00", "30000000.00", LocalDate.of(2027, 12, 31));
        assertThat(GoalProgressCalculator.evaluate(g, AS_OF))
                .isEqualTo(GoalProgressCalculator.evaluate(g, AS_OF));
    }

    @Test
    void storedRatioIsScale4HalfUp_notFloored() {
        // 1/3: the domain value keeps 4 HALF_UP places (0.3333), even though display floors it.
        var p = GoalProgressCalculator.evaluate(goal("3.00", "1.00", null), AS_OF);
        assertThat(p.progress()).isEqualByComparingTo(new BigDecimal("0.3333"));
        assertThat(p.status()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void displayFloorNeverOverstatesCompletion() {
        // Presentation rule (spec §4.2): floor the stored ratio at 2 decimals for display.
        BigDecimal third = GoalProgressCalculator
                .evaluate(goal("3.00", "1.00", null), AS_OF).progress();
        assertThat(third.multiply(new BigDecimal("100"))
                .setScale(2, java.math.RoundingMode.FLOOR).toPlainString())
                .isEqualTo("33.33");

        // 0.9999 must display as 99.99% — never 100% while the goal is still ACTIVE.
        var almost = GoalProgressCalculator.evaluate(goal("10000.00", "9999.00", null), AS_OF);
        assertThat(almost.progress()).isEqualByComparingTo(new BigDecimal("0.9999"));
        assertThat(almost.status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(almost.progress().multiply(new BigDecimal("100"))
                .setScale(2, java.math.RoundingMode.FLOOR).toPlainString())
                .isEqualTo("99.99");
    }
}
