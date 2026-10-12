package com.financialgps.application.goal.usecase;

import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.domain.goal.Goal;

/**
 * Shared Feature 003 lifecycle contract for {@code DEBT_FREE} goals (spec §4.5, decision D-6).
 *
 * <p>A {@code DEBT_FREEDOM} goal is {@code COMPLETED} iff the Feature 002 debt portfolio status is
 * {@code COMPLETED} (no ACTIVE debts remain). The condition is dynamic and not sticky — a new
 * ACTIVE debt makes a {@code COMPLETED} goal {@code ACTIVE} again — so every reader evaluates it
 * against the <em>current</em> portfolio on every read, never trusting a stored flag. Amount-goal
 * lifecycles are already derived from their amounts and pass through unchanged.
 *
 * <p>Both the goal application service ({@link GoalUseCases}) and the GPS goal reader
 * (Feature 004) reconcile through this contract; no reader may reimplement the check inline.
 */
public final class GoalLifecycle {

    private GoalLifecycle() {
    }

    /** {@code true} iff the 002 portfolio projection reports {@code COMPLETED}. */
    public static boolean isPortfolioCompleted(DebtModels.DebtSummaryView summary) {
        return summary != null && summary.portfolioProjection() != null
                && "COMPLETED".equals(summary.portfolioProjection().status());
    }

    /**
     * Reconciles {@code goal} against the current 002 portfolio {@code summary} via
     * {@link Goal#withDebtFreeCompletion(boolean)}. Amount goals are returned unchanged;
     * {@code ARCHIVED} stays terminal (handled in the domain).
     */
    public static Goal reconcile(Goal goal, DebtModels.DebtSummaryView summary) {
        if (goal == null || !goal.isDebtFree()) {
            return goal;
        }
        return goal.withDebtFreeCompletion(isPortfolioCompleted(summary));
    }
}
