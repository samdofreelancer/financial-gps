package com.financialgps.domain.roadmap;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One stage of the route (spec 005 §7.3).
 *
 * <p>Two independent facts are carried side by side and must never be conflated:
 *
 * <ul>
 *   <li>{@code completionReached} — the stage's <b>own</b> completion condition (debt: the 002
 *       portfolio is {@code COMPLETED}; goal: {@code AMOUNT_REACHED}). It never changes because of
 *       route position.</li>
 *   <li>{@code status} — the stage's <b>position</b> on the route. A later stage whose own
 *       condition already holds stays {@link StageStatus#UPCOMING} with {@code blockedBy} set, so
 *       the sequence gate wins for {@code status} while the financial completion is preserved
 *       (RM-006, RM-016).</li>
 * </ul>
 *
 * <p>No field carries an allocation status, a funded amount or a payment shortfall; {@code status}
 * is not a funding statement.
 */
public record RoadmapStage(
        int order,
        StageKind kind,
        UUID goalId,
        String objective,
        String startCondition,
        String completionCondition,
        boolean completionReached,
        Money requiredMonthlyCapacity,
        Money totalPlannedMonthlyPayment,
        BigDecimal progress,
        LocalDate projectedCompletionDate,
        EtaBasis etaBasis,
        EtaAssumption etaAssumption,
        String etaReasonCode,
        StageStatus status,
        Integer blockedBy,
        List<Blocker> blockers,
        String nextAction) {

    public RoadmapStage {
        if (order < 1) {
            throw new IllegalArgumentException("order must be >= 1");
        }
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(objective, "objective");
        Objects.requireNonNull(startCondition, "startCondition");
        Objects.requireNonNull(completionCondition, "completionCondition");
        Objects.requireNonNull(etaBasis, "etaBasis");
        Objects.requireNonNull(etaAssumption, "etaAssumption");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(blockers, "blockers");
        Objects.requireNonNull(nextAction, "nextAction");
        blockers = List.copyOf(blockers);
        if (nextAction.isBlank()) {
            throw new IllegalArgumentException("nextAction must not be blank");
        }
        if (kind == StageKind.DEBT_ELIMINATION) {
            if (goalId != null) {
                throw new IllegalArgumentException("a debt-elimination stage carries no goalId");
            }
            if (requiredMonthlyCapacity != null) {
                throw new IllegalArgumentException("a debt-elimination stage has no required capacity");
            }
            if (progress != null) {
                throw new IllegalArgumentException("a debt-elimination stage has no progress ratio");
            }
        } else {
            if (goalId == null) {
                throw new IllegalArgumentException("a goal stage must carry its goalId");
            }
            if (totalPlannedMonthlyPayment != null) {
                throw new IllegalArgumentException("a goal stage has no planned monthly payment");
            }
        }
        if (progress != null
                && (progress.compareTo(BigDecimal.ZERO) < 0 || progress.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("progress must be within [0, 1]");
        }
        if (projectedCompletionDate == null && etaReasonCode == null) {
            throw new IllegalArgumentException(
                    "an unavailable completion date must carry an etaReasonCode");
        }
        if (projectedCompletionDate != null && etaReasonCode != null) {
            throw new IllegalArgumentException(
                    "an available completion date must not carry an etaReasonCode");
        }
        if ((etaBasis == EtaBasis.PORTFOLIO_PROJECTION)
                != (etaAssumption == EtaAssumption.PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA)) {
            throw new IllegalArgumentException(
                    "debt projection basis and planned-payment assumption travel together");
        }
        if ((etaBasis == EtaBasis.SINGLE_DESTINATION_HYPOTHESIS)
                != (etaAssumption == EtaAssumption.WHOLE_AVAILABLE_CAPACITY)) {
            throw new IllegalArgumentException(
                    "single-destination basis and whole-capacity assumption travel together");
        }
        switch (status) {
            case COMPLETED -> {
                if (blockedBy != null) {
                    throw new IllegalArgumentException("a COMPLETED stage is not blocked");
                }
                if (!completionReached) {
                    throw new IllegalArgumentException("a COMPLETED stage reached its condition");
                }
            }
            case CURRENT -> {
                if (blockedBy != null) {
                    throw new IllegalArgumentException("a CURRENT stage is not blocked");
                }
            }
            case UPCOMING -> {
                if (blockedBy == null) {
                    throw new IllegalArgumentException("an UPCOMING stage names the blocking stage");
                }
                if (blockedBy >= order) {
                    throw new IllegalArgumentException("blockedBy must be an earlier stage");
                }
            }
        }
    }

    /** {@code true} for the single debt-elimination stage of the route. */
    public boolean isDebtStage() {
        return kind == StageKind.DEBT_ELIMINATION;
    }
}
