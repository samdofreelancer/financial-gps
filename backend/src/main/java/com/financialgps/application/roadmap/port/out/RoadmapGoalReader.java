package com.financialgps.application.roadmap.port.out;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.RoadmapInput;

import java.util.List;

/**
 * Port for reading the owner's goals as roadmap facts.
 *
 * <p>Returns the non-archived goals (ACTIVE and COMPLETED) exactly as {@code GoalStore} already
 * orders them — {@code Goal.ORDERING}, nulls first. {@code DEBT_FREEDOM} goals are mapped too: the
 * generator ignores them because their completion condition is the debt stage's, so they never
 * produce a stage of their own.
 */
public interface RoadmapGoalReader {

    List<RoadmapInput.GoalSnapshot> goals(OwnerId owner);
}
