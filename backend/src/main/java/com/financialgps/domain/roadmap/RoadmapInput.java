package com.financialgps.domain.roadmap;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Everything the pure generator may look at (spec 005 §7, data-model §1.4).
 *
 * <p>Facts and already-computed quantities only — never an entity, a repository, an owner-scoped
 * query or a clock. Amounts are decimal strings because the domain is the only lane allowed to
 * build {@code Money}; the route currency is fixed once here and every snapshot is re-expressed in
 * it (single-currency MVP).
 *
 * @param asOf   the evaluation date, passed in explicitly by the application layer
 * @param profile the owner's currency and Available Capacity ({@code availableCapacity} is
 *                {@code null} only when no financial profile exists — capacity is never invented)
 * @param debts   the 002 portfolio facts, or {@code null} when the owner has no debt record at all
 * @param goals   non-archived 003 goals, already owner-scoped; {@code DEBT_FREEDOM} goals are
 *                ignored by the generator because they produce no stage
 */
public record RoadmapInput(
        LocalDate asOf,
        ProfileSnapshot profile,
        DebtPortfolioFacts debts,
        List<GoalSnapshot> goals) {

    public RoadmapInput {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(goals, "goals");
        goals = List.copyOf(goals);
    }

    /** The route currency: the stored profile currency, defaulting exactly like 001 does. */
    public String currency() {
        return profile.currency();
    }

    /**
     * The owner's currency and Available Capacity.
     *
     * @param currency          ISO-4217; never {@code null} (the adapter applies the single 001
     *                          default, so the roadmap never invents a second one)
     * @param availableCapacity decimal string, or {@code null} when it cannot be evaluated
     *                          (missing financial profile) — never a fabricated zero
     */
    public record ProfileSnapshot(String currency, String availableCapacity) {

        public ProfileSnapshot {
            Objects.requireNonNull(currency, "currency");
        }
    }

    /**
     * One 003 goal as facts, in the order {@code GoalStore.findAllByOwner} returned it.
     *
     * @param createdAt 003 ordering key; {@code null} is legal and sorts <em>first</em>
     *                  ({@code Goal.ORDERING}, nulls first)
     */
    public record GoalSnapshot(
            UUID id,
            String name,
            String goalType,
            String completionCondition,
            String targetAmount,
            String currentAmount,
            String targetDate,
            int priority,
            String status,
            Instant createdAt) {

        public GoalSnapshot {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(goalType, "goalType");
            Objects.requireNonNull(completionCondition, "completionCondition");
            Objects.requireNonNull(status, "status");
        }
    }

    /**
     * The 002 plan-based portfolio facts, quoted unchanged.
     *
     * @param status                   {@code AVAILABLE}, {@code BLOCKED} or {@code COMPLETED}
     * @param projectedDebtFreeDate    ISO date from the plan-based projection, or {@code null}
     * @param blockedDebts             one entry per blocked debt (empty when none is blocked)
     */
    public record DebtPortfolioFacts(
            String status,
            String totalOutstandingDebt,
            String totalMinimumMonthlyPayment,
            String totalPlannedMonthlyPayment,
            String projectedDebtFreeDate,
            Integer totalMonthsRemaining,
            String reasonCode,
            String explanation,
            List<BlockedDebt> blockedDebts) {

        public DebtPortfolioFacts {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(totalOutstandingDebt, "totalOutstandingDebt");
            Objects.requireNonNull(totalMinimumMonthlyPayment, "totalMinimumMonthlyPayment");
            Objects.requireNonNull(totalPlannedMonthlyPayment, "totalPlannedMonthlyPayment");
            Objects.requireNonNull(blockedDebts, "blockedDebts");
            blockedDebts = List.copyOf(blockedDebts);
        }

        /** {@code true} when the portfolio still carries ACTIVE debt (so a debt stage exists). */
        public boolean hasActiveDebt() {
            return !"COMPLETED".equals(status);
        }

        public boolean isBlocked() {
            return "BLOCKED".equals(status);
        }

        /** One blocked debt, reusing the 002 reason code as the blocker code. */
        public record BlockedDebt(String creditor, String reasonCode, String explanation) {

            public BlockedDebt {
                Objects.requireNonNull(reasonCode, "reasonCode");
                Objects.requireNonNull(explanation, "explanation");
            }
        }
    }
}
