package com.financialgps.domain.goal;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Pure progress calculator (spec §4.2): remaining = max(target − current, 0); progress 1.0000 iff
 * remaining == 0 else current/target (scale 4, HALF_UP); status COMPLETED iff remaining == 0
 * (ARCHIVED stays terminal).
 */
public final class GoalProgressCalculator {

    private GoalProgressCalculator() {
    }

    public static GoalProgress evaluate(Goal goal, LocalDate asOfDate,
                                        GoalCalculationPolicy policy) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(policy, "policy");
        String currency = goal.targetAmount().currency();
        BigDecimal remainingValue = goal.targetAmount().amount()
                .subtract(goal.currentAmount().amount());
        if (remainingValue.signum() < 0) {
            remainingValue = BigDecimal.ZERO;
        }
        Money remaining = Money.of(
                remainingValue.setScale(policy.monetaryScale(), RoundingMode.HALF_UP).toPlainString(),
                currency);
        BigDecimal progress;
        GoalStatus status;
        if (remaining.amount().signum() == 0) {
            progress = BigDecimal.ONE.setScale(policy.ratioScale());
            status = goal.status() == GoalStatus.ARCHIVED ? GoalStatus.ARCHIVED : GoalStatus.COMPLETED;
        } else {
            if (goal.targetAmount().amount().signum() == 0) {
                progress = BigDecimal.ONE.setScale(policy.ratioScale());
            } else {
                progress = goal.currentAmount().amount()
                        .divide(goal.targetAmount().amount(), policy.ratioScale(), policy.roundingMode());
                if (progress.compareTo(BigDecimal.ZERO) < 0) {
                    progress = BigDecimal.ZERO.setScale(policy.ratioScale());
                }
                if (progress.compareTo(BigDecimal.ONE) >= 0) {
                    progress = BigDecimal.ONE.setScale(policy.ratioScale());
                }
            }
            status = goal.status() == GoalStatus.ARCHIVED ? GoalStatus.ARCHIVED : GoalStatus.ACTIVE;
        }
        return new GoalProgress(remaining, progress, status);
    }

    public static GoalProgress evaluate(Goal goal, LocalDate asOfDate) {
        return evaluate(goal, asOfDate, GoalCalculationPolicy.defaults());
    }
}
