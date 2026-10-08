package com.financialgps.application.goal.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.GetGoals;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStore;
import com.financialgps.domain.model.OwnerId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Read one goal or list the owner's goals, expressed in the position currency. */
public final class GetGoalsUseCase implements GetGoals {

    private final GoalStore goals;
    private final GoalPositionReader positions;
    private final GoalBusinessDate dates;

    public GetGoalsUseCase(GoalStore goals, GoalPositionReader positions, GoalBusinessDate dates) {
        this.goals = goals;
        this.positions = positions;
        this.dates = dates;
    }

    @Override
    public GoalModels.GoalView get(OwnerId owner, UUID id) {
        Goal goal = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        String currency = positions.snapshot(owner).currency();
        return GoalViews.view(goal.withCurrency(currency), dates.today());
    }

    @Override
    public List<GoalModels.GoalView> list(OwnerId owner) {
        LocalDate asOf = dates.today();
        String currency = positions.snapshot(owner).currency();
        List<GoalModels.GoalView> views = new ArrayList<>();
        for (Goal goal : goals.findAllByOwner(owner)) {
            views.add(GoalViews.view(goal.withCurrency(currency), asOf));
        }
        return List.copyOf(views);
    }
}
