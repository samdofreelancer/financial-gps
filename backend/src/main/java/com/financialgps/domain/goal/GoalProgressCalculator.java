package com.financialgps.domain.goal;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Pure progress calculator (spec §4.2) with an explicit two-level rounding rule:
 *
 * <ol>
 *   <li><b>Stored/domain ratio</b>: {@code current / target} at scale 4 with {@code HALF_UP}.
 *       This is the value carried by {@link GoalProgress} and the REST {@code progress} field
 *       (e.g. {@code 1/3 → 0.3333}). It is never floored — flooring the domain value would
 *       change its semantics.</li>
 *   <li><b>Displayed percentage</b>: derived at presentation time by flooring the stored ratio
 *       at 2 decimal places ({@code FLOOR}), so displayed progress never overstates completion
 *       (e.g. stored {@code 0.3333} displays as {@code 33.33%}, never {@code 33.34%}; stored
 *       {@code 0.9999} displays as {@code 99.99%}, never {@code 100%} while still ACTIVE).
 *       The frontend owns this step ({@code GoalProgressCard}); the backend pins the stored
 *       half of the contract here.</li>
 * </ol>
 *
 * {@code remaining = max(target − current, 0)}; progress is {@code 1.0000} iff remaining is 0;
 * status is COMPLETED iff remaining is 0 (ARCHIVED stays terminal).
 */
public final class GoalProgressCalculator {

    private GoalProgressCalculator() {
    }

    public static GoalProgress evaluate(Goal goal, LocalDate asOfDate,
                                        GoalCalculationPolicy policy) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(policy, "policy");
        if (goal.isDebtFree()) {
            // A DEBT_FREE goal's distance is the Feature 002 portfolio outstanding, owned by
            // Feature 004 (engine-contract.md): 003 reports remaining/progress as not applicable.
            // The lifecycle status was already reconciled against the 002 portfolio by the
            // application (Goal.withDebtFreeCompletion).
            return new GoalProgress(null, null, goal.status());
        }
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
