package com.financialgps.domain.goal;

import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * Goal aggregate root (spec §4.1, invariants §8). Immutable; lifecycle transitions return new
 * instances. {@code currentAmount} is user-reported progress for THIS goal only — never derived
 * from the Financial Profile (spec §4.1 anti-duplication).
 */
public final class Goal {

    /**
     * Fallback currency when no Financial Position is available (no profile yet). The application
     * layer always re-expresses goals in the position currency (spec §4.1, §5: currency derived
     * from the owner's profile), so this constant only surfaces for profile-less owners — exactly
     * matching the position reader's own default. Stored amounts are currency-agnostic numerics
     * (no currency column, single-currency MVP).
     */
    public static final String DEFAULT_CURRENCY = "VND";
    public static final String COMPLETION_AMOUNT_REACHED = "AMOUNT_REACHED";

    /** Deterministic list ordering: (priority, createdAt, id) — spec §4.4. */
    public static final Comparator<Goal> ORDERING = Comparator
            .comparingInt(Goal::priority)
            .thenComparing(Goal::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(g -> g.id() == null ? null : g.id().value(),
                    Comparator.nullsFirst(Comparator.naturalOrder()));

    private final GoalId id;
    private final String name;
    private final GoalType goalType;
    private final Money targetAmount;
    private final Money currentAmount;
    private final LocalDate targetDate;
    private final int priority;
    private final String completionCondition;
    private final GoalStatus status;
    private final Instant createdAt;

    public Goal(String name, GoalType goalType, Money targetAmount, Money currentAmount,
                LocalDate targetDate, int priority, String completionCondition, GoalStatus status) {
        this(null, null, name, goalType, targetAmount, currentAmount, targetDate, priority,
                completionCondition, status);
    }

    private Goal(GoalId id, Instant createdAt, String name, GoalType goalType, Money targetAmount,
                 Money currentAmount, LocalDate targetDate, int priority, String completionCondition,
                 GoalStatus status) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("GOAL_NAME_REQUIRED", "Goal name is required");
        }
        if (name.trim().length() > 120) {
            throw new DomainValidationException("GOAL_NAME_TOO_LONG",
                    "Goal name must be at most 120 characters");
        }
        this.id = id;
        this.createdAt = createdAt;
        this.name = name.trim();
        this.goalType = Objects.requireNonNull(goalType, "goalType");
        this.targetAmount = Objects.requireNonNull(targetAmount, "targetAmount");
        this.currentAmount = Objects.requireNonNull(currentAmount, "currentAmount");
        this.targetDate = targetDate;
        if (priority < 1) {
            throw new DomainValidationException("GOAL_PRIORITY_INVALID",
                    "Priority must be at least 1");
        }
        this.priority = priority;
        if (!COMPLETION_AMOUNT_REACHED.equals(completionCondition)) {
            throw new DomainValidationException("GOAL_COMPLETION_INVALID",
                    "Completion condition must be AMOUNT_REACHED");
        }
        this.completionCondition = completionCondition;
        this.status = Objects.requireNonNull(status, "status");
        validate();
    }

    /** New goal fact from user input: status derived from the completion condition (spec §4.2). */
    public static Goal recorded(String currency, String name, String goalType, String targetAmount,
                                String currentAmount, LocalDate targetDate, Integer priority) {
        int prio = priority == null ? 1 : priority;
        GoalType type;
        try {
            type = GoalType.valueOf(goalType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DomainValidationException("GOAL_TYPE_INVALID", "Unknown goal type: " + goalType);
        }
        Money target = Money.of(targetAmount, currency);
        Money current = Money.of(currentAmount, currency);
        boolean completed = target.amount().subtract(current.amount()).signum() <= 0;
        return new Goal(null, null, name, type, target, current, targetDate, prio,
                COMPLETION_AMOUNT_REACHED, completed ? GoalStatus.COMPLETED : GoalStatus.ACTIVE);
    }

    /** Reconstitution for a persisted row. */
    public static Goal reconstitute(GoalId id, Instant createdAt, String currency, String name,
                                    String goalType, String targetAmount, String currentAmount,
                                    LocalDate targetDate, int priority, String completionCondition,
                                    GoalStatus status) {
        return new Goal(id, createdAt, name, GoalType.valueOf(goalType),
                Money.of(targetAmount, currency), Money.of(currentAmount, currency), targetDate,
                priority, completionCondition, status);
    }

    private void validate() {
        if (status == GoalStatus.ARCHIVED) {
            return;
        }
        boolean complete = targetAmount.amount().subtract(currentAmount.amount()).signum() <= 0;
        if (complete && status != GoalStatus.COMPLETED) {
            throw new DomainValidationException("GOAL_STATUS_INCONSISTENT",
                    "A goal with currentAmount >= targetAmount must have status COMPLETED");
        }
        if (!complete && status != GoalStatus.ACTIVE) {
            throw new DomainValidationException("GOAL_STATUS_INCONSISTENT",
                    "A goal with remaining > 0 must have status ACTIVE");
        }
    }

    /** Re-evaluate completion after any amount change: currentAmount >= targetAmount → COMPLETED. */
    public Goal withProgress(Money newTarget, Money newCurrent) {
        boolean complete = newTarget.amount().subtract(newCurrent.amount()).signum() <= 0;
        GoalStatus next = status == GoalStatus.ARCHIVED ? GoalStatus.ARCHIVED
                : (complete ? GoalStatus.COMPLETED : GoalStatus.ACTIVE);
        return new Goal(id, createdAt, name, goalType, newTarget, newCurrent, targetDate, priority,
                completionCondition, next);
    }

    public Goal withDetails(String name, GoalType goalType, Money targetAmount, Money currentAmount,
                            LocalDate targetDate, int priority) {
        boolean complete = targetAmount.amount().subtract(currentAmount.amount()).signum() <= 0;
        GoalStatus next = status == GoalStatus.ARCHIVED ? GoalStatus.ARCHIVED
                : (complete ? GoalStatus.COMPLETED : GoalStatus.ACTIVE);
        return new Goal(id, createdAt, name, goalType, targetAmount, currentAmount, targetDate,
                priority, completionCondition, next);
    }

    public Goal archived() {
        return new Goal(id, createdAt, name, goalType, targetAmount, currentAmount, targetDate,
                priority, completionCondition, GoalStatus.ARCHIVED);
    }

    public Goal withId(GoalId id) {
        return new Goal(Objects.requireNonNull(id, "id"), createdAt, name, goalType, targetAmount,
                currentAmount, targetDate, priority, completionCondition, status);
    }

    public Goal withCreatedAt(Instant createdAt) {
        return new Goal(id, createdAt, name, goalType, targetAmount, currentAmount, targetDate,
                priority, completionCondition, status);
    }

    public Goal withCurrency(String currency) {
        return new Goal(id, createdAt, name, goalType,
                Money.of(targetAmount.asDecimalString(), currency),
                Money.of(currentAmount.asDecimalString(), currency), targetDate, priority,
                completionCondition, status);
    }

    public GoalId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public GoalType goalType() {
        return goalType;
    }

    public Money targetAmount() {
        return targetAmount;
    }

    public Money currentAmount() {
        return currentAmount;
    }

    public LocalDate targetDate() {
        return targetDate;
    }

    public int priority() {
        return priority;
    }

    public String completionCondition() {
        return completionCondition;
    }

    public GoalStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** Stable tie-breaker id for ordering when createdAt collides. */
    public UUID orderId() {
        return id == null ? null : id.value();
    }
}
