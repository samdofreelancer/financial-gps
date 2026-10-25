package com.financialgps.application.goal.port.in;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.domain.model.OwnerId;
import java.util.List;
import java.util.UUID;

public interface GetGoals {
    GoalModels.GoalView get(OwnerId owner, UUID id);

    List<GoalModels.GoalView> list(OwnerId owner);
}
