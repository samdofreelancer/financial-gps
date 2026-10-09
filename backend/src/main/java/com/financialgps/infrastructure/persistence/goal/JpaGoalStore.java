package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStatus;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** JPA adapter for {@link GoalStore}: domain aggregates cross the port, never entities. */
@Component
class JpaGoalStore implements GoalStore {

    private final GoalRepository goals;

    JpaGoalStore(GoalRepository goals) {
        this.goals = goals;
    }

    @Override
    public List<Goal> findAllByOwner(OwnerId owner) {
        List<Goal> mapped = goals
                .findByOwnerIdAndStatusInOrderByPriorityAscCreatedAtAscIdAsc(
                        owner.value(), List.of("ACTIVE", "COMPLETED"))
                .stream().map(JpaGoalStore::toDomain).toList();
        return mapped.stream().sorted(Goal.ORDERING).toList();
    }

    @Override
    public Optional<Goal> findByIdAndOwner(GoalId id, OwnerId owner) {
        return goals.findByIdAndOwnerId(id.value(), owner.value())
                .filter(e -> !"ARCHIVED".equals(e.getStatus()))
                .map(JpaGoalStore::toDomain);
    }

    @Override
    public Goal save(OwnerId owner, Goal goal) {
        GoalEntity entity;
        if (goal.id() == null) {
            entity = new GoalEntity(owner.value(), goal.name(), goal.goalType().name(),
                    amount(goal.targetAmount()), amount(goal.currentAmount()), goal.targetDate(),
                    goal.priority(), goal.completionCondition(), goal.status().name());
        } else {
            entity = goals.findByIdAndOwnerId(goal.id().value(), owner.value())
                    .filter(e -> !"ARCHIVED".equals(e.getStatus()))
                    .orElseThrow(() -> new IllegalStateException(
                            "Goal " + goal.id().value() + " vanished between read and write"));
            entity.setName(goal.name());
            entity.setGoalType(goal.goalType().name());
            entity.setTargetAmount(amount(goal.targetAmount()));
            entity.setCurrentAmount(amount(goal.currentAmount()));
            entity.setTargetDate(goal.targetDate());
            entity.setPriority(goal.priority());
            entity.setCompletionCondition(goal.completionCondition());
            entity.setStatus(goal.status().name());
            entity.touch();
        }
        return toDomain(goals.save(entity));
    }

    @Override
    public boolean archiveByIdAndOwner(GoalId id, OwnerId owner) {
        return goals.archiveByIdAndOwnerId(id.value(), owner.value()) > 0;
    }

    private static BigDecimal amount(com.financialgps.domain.model.Money money) {
        return new BigDecimal(money.asDecimalString());
    }

    private static Goal toDomain(GoalEntity entity) {
        // Stored amounts are currency-agnostic numerics (no currency column, single-currency
        // MVP per owner); the application layer re-expresses them in the position currency via
        // Goal.withCurrency on every read, so the fallback label below never reaches a view.
        Goal goal = Goal.reconstitute(com.financialgps.domain.goal.GoalId.of(entity.getId()),
                entity.getCreatedAt(), Goal.DEFAULT_CURRENCY, entity.getName(),
                entity.getGoalType(), entity.getTargetAmount().toPlainString(),
                entity.getCurrentAmount().toPlainString(), entity.getTargetDate(),
                entity.getPriority(), entity.getCompletionCondition(),
                GoalStatus.valueOf(entity.getStatus()));
        return goal;
    }
}
