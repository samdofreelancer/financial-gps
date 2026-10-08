package com.financialgps.application.goal.port.in;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

public interface CreateGoal {
    GoalModels.GoalView create(OwnerId owner, GoalModels.GoalCommand command);
}
