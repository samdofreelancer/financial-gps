package com.financialgps.application.gps.port.out;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.domain.model.OwnerId;

import java.util.UUID;

/**
 * Port for reading goal data for GPS calculation.
 */
public interface GpsGoalReader {

    GoalModels.GoalView getGoal(OwnerId owner, UUID goalId);
}