package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.application.goal.usecase.GoalLifecycle;
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
 *
 * <p>Feature 004 reuses the Feature 003 lifecycle: a {@code DEBT_FREE} goal is reconciled
 * against the current Feature 002 portfolio through the shared {@link GoalLifecycle} contract
 * (spec §4.5, decision D-6) — dynamic, never a sticky stored flag. The adapter reads no debt
 * arithmetic of its own; it only fetches the already-computed 002 summary.
 */
@Component
public class GpsGoalAdapter implements GpsGoalReader {

    private final GoalStore goals;
    private final GetDebtSummary debts;

    public GpsGoalAdapter(GoalStore goals, GetDebtSummary debts) {
        this.goals = goals;
        this.debts = debts;
    }

    @Override
    public GoalModels.GoalView getGoal(OwnerId owner, UUID goalId, LocalDate asOfDate) {
        Optional<Goal> goalOpt = goals.findByIdAndOwner(GoalId.of(goalId), owner);
        if (goalOpt.isEmpty()) {
            return null;
        }
        // Dynamic DEBT_FREEDOM reconciliation (003 §4.5, D-6): COMPLETED iff the current 002
        // portfolio is COMPLETED; a new ACTIVE debt reactivates the goal. Amount goals pass
        // through unchanged. Evaluated on the GPS asOfDate so the projection stays deterministic.
        Goal goal = GoalLifecycle.reconcile(goalOpt.get(), debts.summary(owner, asOfDate));
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
