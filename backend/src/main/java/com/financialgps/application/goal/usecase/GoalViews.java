package com.financialgps.application.goal.usecase;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Domain projection → application view assembly (no formula here). */
final class GoalViews {

    private GoalViews() {
    }

    /**
     * Assembles the REST view. {@code currency} is the owner's position currency (spec §4.1, §5);
     * for a {@code DEBT_FREE} goal — whose amounts are optional advisory context — it is the only
     * source of the reported currency.
     */
    static GoalModels.GoalView view(Goal goal, String currency, LocalDate asOf) {
        GoalProgress p = GoalProgressCalculator.evaluate(goal, asOf,
                GoalCalculationPolicy.defaults());
        Map<String, String> derived = new LinkedHashMap<>();
        if (goal.targetAmount() != null) {
            derived.put("targetAmount", "actual");
        }
        if (goal.currentAmount() != null) {
            derived.put("currentAmount", "actual");
        }
        if (p.remaining() != null) {
            derived.put("remaining", "calculated");
        }
        if (p.progress() != null) {
            derived.put("progress", "calculated");
        }
        return new GoalModels.GoalView(
                goal.id() == null ? null : goal.id().value().toString(),
                goal.name(), goal.goalType().name(),
                goal.targetAmount() == null ? null : goal.targetAmount().asDecimalString(),
                goal.currentAmount() == null ? null : goal.currentAmount().asDecimalString(),
                goal.targetDate() == null ? null : goal.targetDate().toString(),
                goal.priority(), p.status().name(), currency,
                p.remaining() == null ? null : p.remaining().asDecimalString(),
                p.progress() == null ? null : p.progress().toPlainString(),
                goal.completionCondition(),
                Collections.unmodifiableMap(derived));
    }

    static GoalModels.GoalCapacityView capacityView(Goal goal, GoalCapacity capacity) {
        return new GoalModels.GoalCapacityView(
                goal.id() == null ? null : goal.id().value().toString(),
                capacity.asOf().toString(),
                capacity.remaining() == null ? null : capacity.remaining().asDecimalString(),
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
