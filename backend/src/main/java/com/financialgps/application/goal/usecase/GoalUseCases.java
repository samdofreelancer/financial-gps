package com.financialgps.application.goal.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.CreateGoal;
import com.financialgps.application.goal.port.in.DeleteGoal;
import com.financialgps.application.goal.port.in.GetGoalCapacity;
import com.financialgps.application.goal.port.in.GetGoals;
import com.financialgps.application.goal.port.in.UpdateGoal;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.GetProfile;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.OwnerId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Goal application service: orchestrates persistence, business date, position reader and pure
 * domain calculation. No financial formula lives here — mapping + assembly only. Available
 * Capacity is always read from the Financial Position boundary, never recomputed.
 *
 * <p>Currency (spec §4.1, §5) is derived from the owner's Financial Position on every call:
 * the stored amounts are currency-agnostic numerics (no currency column, single-currency MVP)
 * and are re-expressed in the position currency at the boundary via {@link Goal#withCurrency}.
 * {@link Goal#DEFAULT_CURRENCY} is only the no-profile fallback, matching the position
 * reader's own default.
 */
public final class GoalUseCases implements CreateGoal, UpdateGoal, DeleteGoal, GetGoals, GetGoalCapacity {

    private final GoalStore goals;
    private final GetProfile positions;
    private final GetDebtSummary debts;
    private final GoalBusinessDate dates;

    public GoalUseCases(GoalStore goals, GetProfile positions, GetDebtSummary debts,
                        GoalBusinessDate dates) {
        this.goals = goals;
        this.positions = positions;
        this.debts = debts;
        this.dates = dates;
    }

    @Override
    public GoalModels.GoalView create(OwnerId owner, GoalModels.GoalCommand command) {
        String currency = positionCurrency(owner);
        Goal goal = reconcile(owner, toDomain(currency, command.name(), command.goalType(),
                command.targetAmount(), command.currentAmount(), command.targetDate(),
                command.priority()));
        Goal saved = goals.save(owner, goal);
        // The store reconstitutes with a currency-agnostic label: re-express before rendering.
        return GoalViews.view(saved.withCurrency(currency), currency, dates.today());
    }

    @Override
    public GoalModels.GoalView update(OwnerId owner, UUID id, GoalModels.GoalUpdateCommand command) {
        Goal existing = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        String currency = positionCurrency(owner);
        Goal updated = reconcile(owner, toDomain(currency, command.name(), command.goalType(),
                command.targetAmount(), command.currentAmount(), command.targetDate(),
                command.priority())
                .withId(existing.id())
                .withCreatedAt(existing.createdAt()));
        // Re-evaluate completion on every update: currentAmount >= targetAmount → COMPLETED.
        return GoalViews.view(goals.save(owner, updated).withCurrency(currency), currency,
                dates.today());
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        goals.findByIdAndOwner(GoalId.of(id), owner).orElseThrow(ResourceNotFoundException::new);
        // The pre-check above narrows the race to a concurrent archive between the two
        // statements; a lost race surfaces as 404 instead of a misleading 204.
        if (!goals.archiveByIdAndOwner(GoalId.of(id), owner)) {
            throw new ResourceNotFoundException();
        }
    }

    @Override
    public GoalModels.GoalView get(OwnerId owner, UUID id) {
        Goal goal = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        String currency = positionCurrency(owner);
        return GoalViews.view(reconcile(owner, goal).withCurrency(currency), currency,
                dates.today());
    }

    @Override
    public List<GoalModels.GoalView> list(OwnerId owner) {
        LocalDate asOf = dates.today();
        String currency = positionCurrency(owner);
        List<GoalModels.GoalView> views = new ArrayList<>();
        for (Goal goal : goals.findAllByOwner(owner)) {
            views.add(GoalViews.view(reconcile(owner, goal).withCurrency(currency), currency, asOf));
        }
        return List.copyOf(views);
    }

    @Override
    public GoalModels.GoalCapacityView capacity(OwnerId owner, UUID id) {
        Goal goal = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        ProfileModels.ProfileView position = positions.get(owner);
        String currency = position.currency() == null ? "VND" : position.currency();
        String availableAmount = position.availableCapacity() == null ? "0.00"
                : position.availableCapacity().amount();
        LocalDate snapshotAsOf = LocalDate.parse(position.asOf());
        Goal inPositionCurrency = goal.withCurrency(currency);
        GoalCapacity result;
        try {
            result = GoalCapacityCalculator.evaluate(inPositionCurrency,
                    availableAmount, currency, snapshotAsOf,
                    GoalCalculationPolicy.defaults());
        } catch (DomainValidationException e) {
            throw new GoalValidationException(e.code(), e.getMessage());
        }
        return GoalViews.capacityView(inPositionCurrency, result);
    }

    private String positionCurrency(OwnerId owner) {
        ProfileModels.ProfileView position = positions.get(owner);
        return position.currency() == null ? "VND" : position.currency();
    }

    /**
     * Reconciles a {@code DEBT_FREE} goal against the current Feature 002 portfolio status
     * (spec §4.5, decision D-6). The condition is dynamic and not sticky — a new ACTIVE debt makes
     * a COMPLETED goal ACTIVE again — so this runs on every read, never trusting a stored flag.
     * Amount-goal lifecycles are already derived from their amounts and pass through unchanged.
     */
    private Goal reconcile(OwnerId owner, Goal goal) {
        if (!goal.isDebtFree()) {
            return goal;
        }
        DebtModels.DebtSummaryView summary = debts.summary(owner);
        boolean portfolioCompleted = summary != null && summary.portfolioProjection() != null
                && "COMPLETED".equals(summary.portfolioProjection().status());
        return goal.withDebtFreeCompletion(portfolioCompleted);
    }

    private static Goal toDomain(String currency, String name, String goalType, String targetAmount,
                                 String currentAmount, String targetDate, Integer priority) {
        try {
            if (name == null || goalType == null || currency == null) {
                throw new GoalValidationException("GOAL_REQUIRED",
                        "Name, goal type and currency are required");
            }
            LocalDate date = targetDate == null || targetDate.isBlank() ? null
                    : LocalDate.parse(targetDate);
            return Goal.recorded(currency, name, goalType, targetAmount, currentAmount,
                    date, priority);
        } catch (GoalValidationException e) {
            throw e;
        } catch (DomainValidationException e) {
            throw new GoalValidationException(e.code(), e.getMessage());
        } catch (java.time.DateTimeException e) {
            throw new GoalValidationException("GOAL_DATE_INVALID",
                    "Target date must be a valid ISO-8601 calendar date");
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new GoalValidationException("GOAL_INVALID_TYPE", "Unknown goal type: " + goalType);
        }
    }
}
