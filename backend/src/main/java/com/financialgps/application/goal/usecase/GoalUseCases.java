package com.financialgps.application.goal.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.CreateGoal;
import com.financialgps.application.goal.port.in.DeleteGoal;
import com.financialgps.application.goal.port.in.GetGoalCapacity;
import com.financialgps.application.goal.port.in.GetGoals;
import com.financialgps.application.goal.port.in.UpdateGoal;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStore;
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
 */
public final class GoalUseCases implements CreateGoal, UpdateGoal, DeleteGoal, GetGoals, GetGoalCapacity {

    private final GoalStore goals;
    private final GoalPositionReader positions;
    private final GoalBusinessDate dates;

    public GoalUseCases(GoalStore goals, GoalPositionReader positions, GoalBusinessDate dates) {
        this.goals = goals;
        this.positions = positions;
        this.dates = dates;
    }

    @Override
    public GoalModels.GoalView create(OwnerId owner, GoalModels.GoalCommand command) {
        Goal saved = goals.save(owner, toDomain(command.name(), command.goalType(),
                command.targetAmount(), command.currentAmount(), command.targetDate(),
                command.priority()));
        return GoalViews.view(saved, dates.today());
    }

    @Override
    public GoalModels.GoalView update(OwnerId owner, UUID id, GoalModels.GoalUpdateCommand command) {
        Goal existing = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        Goal updated = toDomain(command.name(), command.goalType(), command.targetAmount(),
                command.currentAmount(), command.targetDate(), command.priority())
                .withId(existing.id())
                .withCreatedAt(existing.createdAt());
        // Re-evaluate completion on every update: currentAmount >= targetAmount → COMPLETED.
        return GoalViews.view(goals.save(owner, updated), dates.today());
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        goals.findByIdAndOwner(GoalId.of(id), owner).orElseThrow(ResourceNotFoundException::new);
        goals.archiveByIdAndOwner(GoalId.of(id), owner);
    }

    @Override
    public GoalModels.GoalView get(OwnerId owner, UUID id) {
        Goal goal = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        return GoalViews.view(goal, dates.today());
    }

    @Override
    public List<GoalModels.GoalView> list(OwnerId owner) {
        LocalDate asOf = dates.today();
        List<GoalModels.GoalView> views = new ArrayList<>();
        for (Goal goal : goals.findAllByOwner(owner)) {
            views.add(GoalViews.view(goal, asOf));
        }
        return List.copyOf(views);
    }

    @Override
    public GoalModels.GoalCapacityView capacity(OwnerId owner, UUID id) {
        Goal goal = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        GoalPositionReader.PositionSnapshot snapshot = positions.snapshot(owner);
        GoalCapacity result = GoalCapacityCalculator.evaluate(goal,
                snapshot.availableCapacityAmount(), snapshot.currency(), snapshot.asOf(),
                GoalCalculationPolicy.defaults());
        return GoalViews.capacityView(goal, result);
    }

    private static Goal toDomain(String name, String goalType, String targetAmount,
                                 String currentAmount, String targetDate, Integer priority) {
        try {
            LocalDate date = targetDate == null || targetDate.isBlank() ? null
                    : LocalDate.parse(targetDate);
            return Goal.recorded(Goal.DEFAULT_CURRENCY, name, goalType, targetAmount, currentAmount,
                    date, priority);
        } catch (DomainValidationException e) {
            throw new GoalValidationException(e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new GoalValidationException("GOAL_INVALID_TYPE", "Unknown goal type: " + goalType);
        }
    }
}
