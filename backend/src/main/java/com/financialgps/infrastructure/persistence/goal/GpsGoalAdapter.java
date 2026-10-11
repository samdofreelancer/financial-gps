package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.application.gps.port.out.GpsGoalReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * GPS adapter for reading goal data.
 */
@Component
public class GpsGoalAdapter implements GpsGoalReader {

    private final GoalStore goals;

    public GpsGoalAdapter(GoalStore goals) {
        this.goals = goals;
    }

    @Override
    public GoalModels.GoalView getGoal(OwnerId owner, UUID goalId, LocalDate asOfDate) {
        Optional<Goal> goalOpt = goals.findByIdAndOwner(GoalId.of(goalId), owner);
        if (goalOpt.isEmpty()) {
            return null;
        }
        Goal goal = goalOpt.get();
        // Currency will be overridden by the application layer to match position currency
        String currency = "VND";
        // Remaining/progress follow the Feature 003 domain rules (zero-target safe,
        // clamped, debt-freedom not applicable) — no duplicated calculation here.
        GoalProgress evaluated = GoalProgressCalculator.evaluate(goal, asOfDate,
                GoalCalculationPolicy.defaults());
        String remaining = evaluated.remaining() != null
                ? evaluated.remaining().asDecimalString()
                : null;
        String progress = evaluated.progress() != null
                ? evaluated.progress().toPlainString()
                : null;
        return new GoalModels.GoalView(
                goal.id().value().toString(),
                goal.name(),
                goal.goalType().name(),
                goal.targetAmount() != null ? goal.targetAmount().asDecimalString() : null,
                goal.currentAmount() != null ? goal.currentAmount().asDecimalString() : null,
                goal.targetDate() != null ? goal.targetDate().toString() : null,
                goal.priority(),
                evaluated.status().name(),
                currency,
                remaining,
                progress,
                goal.completionCondition(),
                java.util.Map.of()
        );
    }
}