package com.financialgps.application.goal.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.UpdateGoal;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStore;
import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

/**
 * Replace a goal fact, preserving identity. Completion is re-evaluated on every update, so
 * {@code currentAmount >= targetAmount} always yields {@code COMPLETED}.
 */
public final class UpdateGoalUseCase implements UpdateGoal {

    private final GoalStore goals;
    private final GoalPositionReader positions;
    private final GoalBusinessDate dates;

    public UpdateGoalUseCase(GoalStore goals, GoalPositionReader positions, GoalBusinessDate dates) {
        this.goals = goals;
        this.positions = positions;
        this.dates = dates;
    }

    @Override
    public GoalModels.GoalView update(OwnerId owner, UUID id, GoalModels.GoalUpdateCommand command) {
        Goal existing = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        String currency = positions.snapshot(owner).currency();
        Goal updated = GoalCommandMapper.toDomain(currency, command.name(), command.goalType(),
                command.targetAmount(), command.currentAmount(), command.targetDate(),
                command.priority())
                .withId(existing.id())
                .withCreatedAt(existing.createdAt());
        return GoalViews.view(goals.save(owner, updated).withCurrency(currency), dates.today());
    }
}
