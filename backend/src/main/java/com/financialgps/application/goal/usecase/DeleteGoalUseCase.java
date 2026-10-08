package com.financialgps.application.goal.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.goal.port.in.DeleteGoal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStore;
import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

/** Soft-delete a goal to ARCHIVED. A lost archive race answers 404, never a misleading 204. */
public final class DeleteGoalUseCase implements DeleteGoal {

    private final GoalStore goals;

    public DeleteGoalUseCase(GoalStore goals) {
        this.goals = goals;
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        goals.findByIdAndOwner(GoalId.of(id), owner).orElseThrow(ResourceNotFoundException::new);
        if (!goals.archiveByIdAndOwner(GoalId.of(id), owner)) {
            throw new ResourceNotFoundException();
        }
    }
}
