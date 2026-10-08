package com.financialgps.application.goal.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.in.GetGoalCapacity;
import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStore;
import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

/**
 * Required monthly capacity of one goal against Available Capacity. The capacity figure is
 * always read from the Financial Position boundary, never recomputed here.
 */
public final class GetGoalCapacityUseCase implements GetGoalCapacity {

    private final GoalStore goals;
    private final GoalPositionReader positions;

    public GetGoalCapacityUseCase(GoalStore goals, GoalPositionReader positions) {
        this.goals = goals;
        this.positions = positions;
    }

    @Override
    public GoalModels.GoalCapacityView capacity(OwnerId owner, UUID id) {
        Goal goal = goals.findByIdAndOwner(GoalId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        GoalPositionReader.PositionSnapshot snapshot = positions.snapshot(owner);
        Goal inPositionCurrency = goal.withCurrency(snapshot.currency());
        GoalCapacity result;
        try {
            result = GoalCapacityCalculator.evaluate(inPositionCurrency,
                    snapshot.availableCapacityAmount(), snapshot.currency(), snapshot.asOf(),
                    GoalCalculationPolicy.defaults());
        } catch (DomainValidationException e) {
            throw new GoalValidationException(e.code(), e.getMessage());
        }
        return GoalViews.capacityView(inPositionCurrency, result);
    }
}
