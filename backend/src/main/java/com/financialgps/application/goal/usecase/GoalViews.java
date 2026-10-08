package com.financialgps.application.goal.usecase;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import java.time.LocalDate;
import java.util.Map;

/** Domain projection → application view assembly (no formula here). */
final class GoalViews {

    private GoalViews() {
    }

    static GoalModels.GoalView view(Goal goal, LocalDate asOf) {
        GoalProgress p = GoalProgressCalculator.evaluate(goal, asOf,
                GoalCalculationPolicy.defaults());
        return new GoalModels.GoalView(
                goal.id() == null ? null : goal.id().value().toString(),
                goal.name(), goal.goalType().name(),
                goal.targetAmount().asDecimalString(), goal.currentAmount().asDecimalString(),
                goal.targetDate() == null ? null : goal.targetDate().toString(),
                goal.priority(), p.status().name(), goal.targetAmount().currency(),
                p.remaining().asDecimalString(), p.progress().toPlainString(),
                goal.completionCondition(),
                Map.of("remaining", "calculated", "progress", "calculated",
                        "targetAmount", "actual", "currentAmount", "actual"));
    }

    static GoalModels.GoalCapacityView capacityView(Goal goal, GoalCapacity capacity) {
        return new GoalModels.GoalCapacityView(
                goal.id() == null ? null : goal.id().value().toString(),
                capacity.asOf().toString(),
                capacity.remaining().asDecimalString(),
                capacity.monthsRemaining(),
                capacity.requiredMonthlyCapacity() == null ? null
                        : capacity.requiredMonthlyCapacity().asDecimalString(),
                capacity.availableCapacity().asDecimalString(),
                capacity.capacityCoverage().name(),
                capacity.monthlyShortfall() == null ? null
                        : capacity.monthlyShortfall().asDecimalString(),
                capacity.dateFeasibility().name(),
                capacity.explanation());
    }
}
