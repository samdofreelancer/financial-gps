package com.financialgps.application.goal.port.out;

import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.model.OwnerId;

import java.util.List;
import java.util.Optional;

/**
 * Goal repository port: owner-scoped by construction, operating on domain {@link Goal}
 * aggregates only. Soft-delete only — the adapter transitions status to ARCHIVED.
 */
public interface GoalStore {

    /** All non-archived goals of the owner (ACTIVE + COMPLETED), ordered by (priority, createdAt, id). */
    List<Goal> findAllByOwner(OwnerId owner);

    Optional<Goal> findByIdAndOwner(GoalId id, OwnerId owner);

    /** Create or update; returns the stored aggregate with its id assigned. */
    Goal save(OwnerId owner, Goal goal);

    boolean archiveByIdAndOwner(GoalId id, OwnerId owner);
}
