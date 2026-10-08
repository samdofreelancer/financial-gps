package com.financialgps.application.goal.usecase;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.CreateGoal;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalStore;
import com.financialgps.domain.model.OwnerId;

/** Create a goal fact: completion derived from amounts, currency from the position. */
public final class CreateGoalUseCase implements CreateGoal {

    private final GoalStore goals;
    private final GoalPositionReader positions;
    private final GoalBusinessDate dates;

    public CreateGoalUseCase(GoalStore goals, GoalPositionReader positions, GoalBusinessDate dates) {
        this.goals = goals;
        this.positions = positions;
        this.dates = dates;
    }

    @Override
    public GoalModels.GoalView create(OwnerId owner, GoalModels.GoalCommand command) {
        String currency = positions.snapshot(owner).currency();
        Goal saved = goals.save(owner, GoalCommandMapper.toDomain(currency, command.name(),
                command.goalType(), command.targetAmount(), command.currentAmount(),
                command.targetDate(), command.priority()));
        // The store reconstitutes with a currency-agnostic label: re-express before rendering.
        return GoalViews.view(saved.withCurrency(currency), dates.today());
    }
}
