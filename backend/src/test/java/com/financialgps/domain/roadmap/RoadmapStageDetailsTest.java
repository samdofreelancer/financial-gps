package com.financialgps.domain.roadmap;

import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T032 — US2 stage details (spec 005 §12: RM-006 … RM-009, RM-015 … RM-018).
 *
 * <p>Every stage must say what completes it, what blocks the next one and what to do next — without
 * ever transporting a funded amount, a capacity shortfall or an allocation status.
 */
class RoadmapStageDetailsTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 22);
    private static final UUID GOAL_ID = UUID.fromString("6f1c9d54-2f0b-4a1e-9c33-0b7d5a1e9f42");
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    private static RoadmapInput.GoalSnapshot goal(String type, String target, String current,
                                                  String targetDate) {
        return new RoadmapInput.GoalSnapshot(
                GOAL_ID,
                "Emergency fund",
                type,
                Goal.completionConditionFor(GoalType.valueOf(type)),
                target,
                current,
                targetDate,
                1,
                "ACTIVE",
                CREATED);
    }

    /** A goal whose own amount condition already holds — stored COMPLETED, as 003 does. */
    private static RoadmapInput.GoalSnapshot reachedGoal(String type, String target,
                                                         String targetDate) {
        return new RoadmapInput.GoalSnapshot(
                GOAL_ID,
                "Emergency fund",
                type,
                Goal.completionConditionFor(GoalType.valueOf(type)),
                target,
                target,
                targetDate,
                1,
                "COMPLETED",
                CREATED);
    }

    private static RoadmapInput.DebtPortfolioFacts debt(String status, String projectedDate,
                                                        String reasonCode,
                                                        List<RoadmapInput.DebtPortfolioFacts.BlockedDebt> blocked) {
        return new RoadmapInput.DebtPortfolioFacts(
                status,
                "100000000.00",
                "5000000.00",
                "7000000.00",
                projectedDate,
                projectedDate == null ? null : 18,
                reasonCode,
                "At least one active debt cannot be projected to payoff; the portfolio debt-free "
                        + "date is unavailable.",
                blocked);
    }

    private static RoadmapInput.DebtPortfolioFacts activeDebt() {
        return debt("AVAILABLE", "2027-04-30", null, List.of());
    }

    private static Roadmap generate(String capacity, RoadmapInput.DebtPortfolioFacts debts,
                                    RoadmapInput.GoalSnapshot... goals) {
        return RoadmapGenerator.generate(new RoadmapInput(
                AS_OF, new RoadmapInput.ProfileSnapshot("VND", capacity), debts, List.of(goals)));
    }

    private static RoadmapStage onlyGoalStage(Roadmap roadmap) {
        return roadmap.stages().stream().filter(stage -> !stage.isDebtStage()).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- FR-002: conditions

    @Test
    void debtStageStatesItsStartAndCompletionConditions() {
        RoadmapStage stage = generate("31000000.00", activeDebt()).stages().get(0);

        assertThat(stage.startCondition()).isEqualTo(RoadmapStageProjector.DEBT_START_CONDITION);
        assertThat(stage.completionCondition()).isEqualTo(RoadmapStageProjector.DEBT_COMPLETION_CONDITION);
        assertThat(stage.completionReached()).isFalse();
        assertThat(stage.status()).isEqualTo(StageStatus.CURRENT);
        assertThat(stage.nextAction()).contains("planned payment");
    }

    @Test
    void goalStageStatesTheGoalConditionAndThatTheStartIsAPositionOnly() {
        RoadmapStage stage = onlyGoalStage(generate("31000000.00", activeDebt(),
                goal("EMERGENCY_FUND", "60000000.00", "10000000.00", "2027-10-22")));

        assertThat(stage.completionCondition()).isEqualTo(Goal.COMPLETION_AMOUNT_REACHED);
        assertThat(stage.startCondition()).isEqualTo(RoadmapStageProjector.GOAL_START_CONDITION);
        assertThat(stage.startCondition()).contains("Roadmap position only");
    }

    // ---------------------------------------------------------------- RM-007 / RM-008 / RM-009: blockers and unavailable ETAs

    @Test
    void aBlockedPortfolioBlocksTheDebtStageAndCarriesThe002ReasonCode() {
        RoadmapInput.DebtPortfolioFacts blocked = debt("BLOCKED", null,
                "PORTFOLIO_CONTAINS_BLOCKED_DEBTS",
                List.of(new RoadmapInput.DebtPortfolioFacts.BlockedDebt("Bank",
                        "INTEREST_RATE_MISSING", "The interest rate is required to project this debt.")));

        RoadmapStage stage = generate("31000000.00", blocked).stages().get(0);

        assertThat(stage.projectedCompletionDate()).isNull();
        assertThat(stage.etaReasonCode()).isEqualTo("PORTFOLIO_CONTAINS_BLOCKED_DEBTS");
        assertThat(stage.etaBasis()).isEqualTo(EtaBasis.PORTFOLIO_PROJECTION);
        assertThat(stage.blockers()).extracting(Blocker::code).containsExactly("INTEREST_RATE_MISSING");
        assertThat(stage.nextAction()).contains("Resolve the blocked debts first");
    }

    @Test
    void rm009MissingProfileIsReportedAsABlockerAndAnUnavailableEta() {
        Roadmap roadmap = generate(null, null,
                goal("EMERGENCY_FUND", "60000000.00", "0.00", "2027-10-22"));
        RoadmapStage stage = onlyGoalStage(roadmap);

        assertThat(stage.projectedCompletionDate()).isNull();
        assertThat(stage.etaReasonCode()).isEqualTo("MISSING_FINANCIAL_PROFILE");
        assertThat(stage.blockers()).extracting(Blocker::code).containsExactly("MISSING_FINANCIAL_PROFILE");
        assertThat(stage.nextAction()).contains("Complete the Financial Profile");
        assertThat(roadmap.explanations())
                .anySatisfy(line -> assertThat(line).contains("MISSING_FINANCIAL_PROFILE"));
        // Required capacity comes from 003 and does not depend on the missing capacity operand.
        assertThat(stage.requiredMonthlyCapacity().asDecimalString()).isEqualTo("5000000.00");
    }

    @Test
    void rm008ZeroCapacityIsReportedAsABlockerAndAnUnavailableEta() {
        Roadmap roadmap = generate("0.00", null,
                goal("EMERGENCY_FUND", "60000000.00", "0.00", "2027-10-22"));
        RoadmapStage stage = onlyGoalStage(roadmap);

        assertThat(stage.projectedCompletionDate()).isNull();
        assertThat(stage.etaReasonCode()).isEqualTo("NO_AVAILABLE_CAPACITY");
        assertThat(stage.blockers()).extracting(Blocker::code).containsExactly("NO_AVAILABLE_CAPACITY");
        assertThat(stage.nextAction()).contains("Increase Net Cash Flow");
    }

    @Test
    void aHorizonBeyondTheRepresentableMaximumFabricatesNoDate() {
        Roadmap roadmap = generate("0.01", null,
                goal("SAVINGS", "30000000.00", "0.00", "2027-10-22"));
        RoadmapStage stage = onlyGoalStage(roadmap);

        assertThat(stage.projectedCompletionDate()).isNull();
        assertThat(stage.etaReasonCode()).isEqualTo("PAYOFF_HORIZON_EXCEEDS_MAXIMUM");
        assertThat(stage.blockers()).extracting(Blocker::code)
                .containsExactly("PAYOFF_HORIZON_EXCEEDS_MAXIMUM");
    }

    @Test
    void rm007AnUndatedGoalHasNoRequiredCapacityButStillHasAHypothesisDate() {
        Roadmap roadmap = generate("31000000.00", null,
                goal("EMERGENCY_FUND", "60000000.00", "0.00", null));
        RoadmapStage stage = onlyGoalStage(roadmap);

        assertThat(stage.requiredMonthlyCapacity()).isNull();
        assertThat(stage.projectedCompletionDate()).isEqualTo(AS_OF.plusMonths(2));
        assertThat(stage.nextAction()).contains("Supply a target date");
        assertThat(roadmap.explanations())
                .anySatisfy(line -> assertThat(line).contains("UNDATED_GOAL"));
    }

    // ---------------------------------------------------------------- RM-016: next action / explanations

    @Test
    void rm016AnUpcomingGoalExplainsTheEarlierStageAndThatPositionIsNotFunding() {
        Roadmap roadmap = generate("31000000.00", activeDebt(),
                goal("EMERGENCY_FUND", "60000000.00", "0.00", "2027-10-22"));
        RoadmapStage upcoming = onlyGoalStage(roadmap);

        assertThat(upcoming.status()).isEqualTo(StageStatus.UPCOMING);
        assertThat(upcoming.blockedBy()).isEqualTo(1);
        assertThat(upcoming.nextAction())
                .contains("Stage 1 must complete")
                .contains(RoadmapStageDetailFactory.NOT_FUNDING_STATEMENT);
        assertThat(roadmap.explanations()).anySatisfy(line -> assertThat(line)
                .contains("Stage 2 is not current because stage 1 is not complete yet"));
        assertThat(roadmap.explanations()).anySatisfy(line -> assertThat(line)
                .contains("not an allocation order"));
    }

    @Test
    void aCompletedStageStatesThatItsObjectiveIsMet() {
        Roadmap roadmap = generate("31000000.00", null,
                reachedGoal("EMERGENCY_FUND", "60000000.00", "2027-10-22"));

        assertThat(roadmap.stages().get(0).status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(roadmap.stages().get(0).nextAction()).contains("objective is met");
    }

    // ---------------------------------------------------------------- RM-015: no shortfall amount leaks

    @Test
    void rm015TheCapacityShortfallAmountNeverReachesTheStageOrTheRouteText() {
        // Required 5,000,000/month vs Available Capacity 1,000,000 → 004 shortfall 4,000,000.
        Roadmap roadmap = generate("1000000.00", activeDebt(),
                goal("EMERGENCY_FUND", "60000000.00", "0.00", "2027-10-22"));
        RoadmapStage stage = onlyGoalStage(roadmap);

        assertThat(stage.nextAction()).doesNotContain("shortfall").doesNotContain("4000000");
        assertThat(roadmap.explanations()).allSatisfy(line ->
                assertThat(line).doesNotContain("4000000").doesNotContain("shortfall"));
        assertThat(roadmap.provenance()).allSatisfy(entry ->
                assertThat(entry.detail()).doesNotContain("4000000"));
    }

    // ---------------------------------------------------------------- RM-016: position versus completion

    @Test
    void rm016AReachedUpcomingStageShowsCompletionBesideItsPosition() {
        Roadmap roadmap = generate("31000000.00", activeDebt(),
                reachedGoal("EMERGENCY_FUND", "60000000.00", "2027-10-22"));
        RoadmapStage stage = onlyGoalStage(roadmap);

        assertThat(stage.completionReached()).isTrue();
        assertThat(stage.status()).isEqualTo(StageStatus.UPCOMING);
        assertThat(stage.nextAction()).contains("already reached its target");
    }

    // ---------------------------------------------------------------- RM-017 / RM-018: ETA honesty

    @Test
    void rm017TheDebtEtaIsPlanBasedAndSaysSo() {
        Roadmap roadmap = generate("31000000.00", activeDebt());
        RoadmapStage stage = roadmap.stages().get(0);

        assertThat(stage.etaBasis()).isEqualTo(EtaBasis.PORTFOLIO_PROJECTION);
        assertThat(stage.etaAssumption()).isEqualTo(EtaAssumption.PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA);
        assertThat(roadmap.explanations()).anySatisfy(line -> assertThat(line)
                .contains("002 plan-based projection")
                .contains("including each planned Debt Extra"));
        assertThat(roadmap.provenance()).anySatisfy(entry -> assertThat(entry.detail())
                .contains("plan-based portfolio projection"));
        assertThat(stage.nextAction()).doesNotContain("funded");
    }

    @Test
    void rm018V1NeverEmitsTheReservedRouteAwareLabels() {
        Roadmap roadmap = generate("31000000.00", activeDebt(),
                goal("EMERGENCY_FUND", "60000000.00", "0.00", "2027-10-22"));

        assertThat(roadmap.stages()).allSatisfy(stage -> {
            assertThat(stage.etaBasis()).isNotEqualTo(EtaBasis.ROUTE_PROJECTION);
            assertThat(stage.etaAssumption()).isNotEqualTo(EtaAssumption.ROUTE_ALLOCATION);
        });
        assertThat(roadmap.provenance()).noneMatch(entry ->
                entry.detail().contains("ROUTE_PROJECTION") || entry.detail().contains("ROUTE_ALLOCATION"));
    }

    @Test
    void kindDependentCapacityFiguresAreMutuallyExclusive() {
        Roadmap roadmap = generate("31000000.00", activeDebt(),
                goal("EMERGENCY_FUND", "60000000.00", "0.00", "2027-10-22"));

        RoadmapStage debtStage = roadmap.stages().get(0);
        RoadmapStage goalStage = roadmap.stages().get(1);

        assertThat(debtStage.totalPlannedMonthlyPayment()).isNotNull();
        assertThat(debtStage.requiredMonthlyCapacity()).isNull();
        assertThat(goalStage.requiredMonthlyCapacity()).isNotNull();
        assertThat(goalStage.totalPlannedMonthlyPayment()).isNull();
    }
}
