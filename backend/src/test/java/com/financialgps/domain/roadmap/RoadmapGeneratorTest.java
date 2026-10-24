package com.financialgps.domain.roadmap;

import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T011 — US1 ordering and current-stage rules (spec 005 §12, RM-001 … RM-006, RM-010 … RM-013,
 * RM-016 plus FR-009 determinism).
 *
 * <p>The roadmap stage order is a <b>presentation</b> order: debt elimination → emergency fund →
 * remaining goals. It is never the allocation order, and a stage's position never gates funding.
 */
class RoadmapGeneratorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 22);
    private static final String CURRENCY = "VND";
    private static final String CAPACITY = "31000000.00";
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    private static final UUID EMERGENCY_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID SAVINGS_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID HOUSING_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID EDUCATION_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");

    // ---------------------------------------------------------------- fixtures

    private static RoadmapInput.ProfileSnapshot profile(String capacity) {
        return new RoadmapInput.ProfileSnapshot(CURRENCY, capacity);
    }

    private static RoadmapInput.GoalSnapshot goal(UUID id, String name, String type, String target,
                                                  String current, String targetDate, int priority,
                                                  String status, Instant createdAt) {
        return new RoadmapInput.GoalSnapshot(
                id,
                name,
                type,
                Goal.completionConditionFor(GoalType.valueOf(type)),
                target,
                current,
                targetDate,
                priority,
                status,
                createdAt);
    }

    private static RoadmapInput.GoalSnapshot activeGoal(UUID id, String name, String type,
                                                        String target, String current,
                                                        String targetDate, int priority) {
        return goal(id, name, type, target, current, targetDate, priority, "ACTIVE", CREATED);
    }

    private static RoadmapInput.GoalSnapshot completedGoal(UUID id, String name, String type,
                                                           String target, String targetDate) {
        return goal(id, name, type, target, target, targetDate, 1, "COMPLETED", CREATED);
    }

    private static RoadmapInput.DebtPortfolioFacts activeDebt() {
        return new RoadmapInput.DebtPortfolioFacts(
                "AVAILABLE",
                "100000000.00",
                "5000000.00",
                "7000000.00",
                "2027-04-30",
                18,
                null,
                "All active debts will be paid off by 2027-04-30 under planned payments.",
                List.of());
    }

    private static RoadmapInput.DebtPortfolioFacts noDebt() {
        return new RoadmapInput.DebtPortfolioFacts(
                "COMPLETED",
                "0.00",
                "0.00",
                "0.00",
                null,
                0,
                "DEBT_ALREADY_PAID",
                "No active debts: the portfolio is already debt-free.",
                List.of());
    }

    private static RoadmapInput input(RoadmapInput.DebtPortfolioFacts debts,
                                      RoadmapInput.GoalSnapshot... goals) {
        return new RoadmapInput(AS_OF, profile(CAPACITY), debts, List.of(goals));
    }

    private static Roadmap generate(RoadmapInput.DebtPortfolioFacts debts,
                                    RoadmapInput.GoalSnapshot... goals) {
        return RoadmapGenerator.generate(input(debts, goals));
    }

    private static List<String> kinds(Roadmap roadmap) {
        return roadmap.stages().stream().map(stage -> stage.kind().name()).toList();
    }

    private static List<String> objectives(Roadmap roadmap) {
        return roadmap.stages().stream().map(RoadmapStage::objective).toList();
    }

    // ---------------------------------------------------------------- RM-002 / RM-003: stage set and tier order

    @Test
    void rm002OrdersDebtThenEmergencyFundThenRemainingGoalsByPriority() {
        Roadmap roadmap = generate(activeDebt(),
                activeGoal(SAVINGS_ID, "Savings", "SAVINGS", "90000000.00", "0.00",
                        "2027-12-31", 1),
                activeGoal(HOUSING_ID, "Housing", "HOUSING", "90000000.00", "0.00",
                        "2027-12-31", 2),
                activeGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "0.00", "2027-10-22", 9));

        assertThat(kinds(roadmap)).containsExactly("DEBT_ELIMINATION", "GOAL", "GOAL", "GOAL");
        assertThat(objectives(roadmap)).containsExactly(
                "Eliminate the outstanding debt", "Emergency fund", "Savings", "Housing");
        assertThat(roadmap.stages()).extracting(RoadmapStage::order).containsExactly(1, 2, 3, 4);
        assertThat(roadmap.orderPolicy().scope()).isEqualTo(OrderPolicy.SCOPE_ROADMAP_STAGE_ORDER);
        assertThat(roadmap.orderPolicy().appliedOverrides()).isEmpty();
    }

    @Test
    void debtStageCarriesThePlannedPaymentAndNeverACapacityOrProgress() {
        Roadmap roadmap = generate(activeDebt());
        RoadmapStage debt = roadmap.stages().get(0);

        assertThat(debt.totalPlannedMonthlyPayment().asDecimalString()).isEqualTo("7000000.00");
        assertThat(debt.requiredMonthlyCapacity()).isNull();
        assertThat(debt.progress()).isNull();
        assertThat(debt.goalId()).isNull();
        assertThat(debt.etaBasis()).isEqualTo(EtaBasis.PORTFOLIO_PROJECTION);
        assertThat(debt.etaAssumption()).isEqualTo(EtaAssumption.PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA);
        assertThat(debt.projectedCompletionDate()).isEqualTo(LocalDate.of(2027, 4, 30));
    }

    // ---------------------------------------------------------------- RM-001 / RM-006: current stage + sequence gate

    @Test
    void rm001ASingleGoalIsTheCurrentStage() {
        Roadmap roadmap = generate(noDebt(),
                activeGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "0.00", "2027-10-22", 1));

        assertThat(roadmap.stages()).hasSize(1);
        assertThat(roadmap.stages().get(0).kind()).isEqualTo(StageKind.GOAL);
        assertThat(roadmap.stages().get(0).status()).isEqualTo(StageStatus.CURRENT);
        assertThat(roadmap.routeStatus()).isEqualTo(RouteStatus.IN_PROGRESS);
        assertThat(roadmap.currentStageOrder()).isEqualTo(1);
    }

    @Test
    void currentStageIsTheFirstIncompleteStageAndIsUnique() {
        Roadmap roadmap = generate(activeDebt(),
                activeGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "0.00", "2027-10-22", 1));

        assertThat(roadmap.routeStatus()).isEqualTo(RouteStatus.IN_PROGRESS);
        assertThat(roadmap.currentStageOrder()).isEqualTo(1);
        assertThat(roadmap.stages()).extracting(RoadmapStage::status)
                .containsExactly(StageStatus.CURRENT, StageStatus.UPCOMING);
        assertThat(roadmap.stages().get(1).blockedBy()).isEqualTo(1);
    }

    @Test
    void rm006ALaterStageIsNeverPositionallyCompleteBeforeAnEarlierStage() {
        Roadmap roadmap = generate(activeDebt(),
                completedGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "2027-10-22"),
                activeGoal(SAVINGS_ID, "Savings", "SAVINGS", "90000000.00", "0.00",
                        "2027-12-31", 2));

        assertThat(roadmap.stages()).extracting(RoadmapStage::status).containsExactly(
                StageStatus.CURRENT, StageStatus.UPCOMING, StageStatus.UPCOMING);
        assertThat(roadmap.currentStageOrder()).isEqualTo(1);
        assertThat(roadmap.stages().get(1).blockedBy()).isEqualTo(1);
        assertThat(roadmap.stages().get(2).blockedBy()).isEqualTo(1);
    }

    @Test
    void rm006WhenTheFirstStageCompletesTheSecondBecomesCurrent() {
        Roadmap roadmap = generate(noDebt(),
                completedGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "2027-10-22"),
                activeGoal(SAVINGS_ID, "Savings", "SAVINGS", "90000000.00", "0.00",
                        "2027-12-31", 2));

        assertThat(roadmap.stages()).extracting(RoadmapStage::status)
                .containsExactly(StageStatus.COMPLETED, StageStatus.CURRENT);
        assertThat(roadmap.currentStageOrder()).isEqualTo(2);
        assertThat(roadmap.stages().get(0).completionReached()).isTrue();
        assertThat(roadmap.stages().get(1).blockedBy()).isNull();
    }

    // ---------------------------------------------------------------- RM-006: own completion beside position

    @Test
    void rm006GoalCompletionReachedFollowsTheGoalsOwnCondition() {
        Roadmap roadmap = generate(noDebt(),
                completedGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "2027-10-22"),
                activeGoal(SAVINGS_ID, "Savings", "SAVINGS", "90000000.00", "45000000.00",
                        "2027-12-31", 2));

        RoadmapStage reached = roadmap.stages().get(0);
        RoadmapStage active = roadmap.stages().get(1);

        assertThat(reached.completionReached()).isTrue();
        assertThat(reached.progress().toPlainString()).isEqualTo("1.0000");
        assertThat(reached.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(active.completionReached()).isFalse();
        assertThat(active.progress().toPlainString()).isEqualTo("0.5000");
        assertThat(active.status()).isEqualTo(StageStatus.CURRENT);
    }

    @Test
    void rm006AReachedGoalHeldBehindAnIncompleteStageKeepsCompletionBesideUpcoming() {
        Roadmap roadmap = generate(activeDebt(),
                completedGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "2027-10-22"));

        RoadmapStage reached = roadmap.stages().get(1);
        assertThat(reached.completionReached()).isTrue();
        assertThat(reached.status()).isEqualTo(StageStatus.UPCOMING);
        assertThat(reached.blockedBy()).isEqualTo(1);
        assertThat(reached.nextAction()).contains("already reached its target");
        assertThat(roadmap.explanations())
                .anySatisfy(line -> assertThat(line)
                        .contains("Stage 2 has already reached its own completion condition"));
    }

    // ---------------------------------------------------------------- FR-002: forwarded 003 quantities

    @Test
    void goalStageForwardsProgressAndRequiredCapacityFrom003() {
        Roadmap roadmap = generate(noDebt(),
                activeGoal(SAVINGS_ID, "Savings", "SAVINGS", "120000000.00", "30000000.00",
                        AS_OF.plusMonths(12).toString(), 1));

        RoadmapStage stage = roadmap.stages().get(0);
        assertThat(stage.progress().toPlainString()).isEqualTo("0.2500");
        assertThat(stage.requiredMonthlyCapacity().asDecimalString()).isEqualTo("7500000.00");
        assertThat(stage.goalId()).isEqualTo(SAVINGS_ID);
        assertThat(stage.totalPlannedMonthlyPayment()).isNull();
        assertThat(stage.etaBasis()).isEqualTo(EtaBasis.SINGLE_DESTINATION_HYPOTHESIS);
        assertThat(stage.etaAssumption()).isEqualTo(EtaAssumption.WHOLE_AVAILABLE_CAPACITY);
        // 90M remaining / 31M capacity → ceil(2.90) = 3 periods.
        assertThat(stage.projectedCompletionDate()).isEqualTo(AS_OF.plusMonths(3));
    }

    // ---------------------------------------------------------------- RM-005 / RM-004 / RM-012: route status

    @Test
    void rm005NoDebtAndNoGoalIsNoDestination() {
        Roadmap roadmap = generate(noDebt());

        assertThat(roadmap.routeStatus()).isEqualTo(RouteStatus.NO_DESTINATION);
        assertThat(roadmap.stages()).isEmpty();
        assertThat(roadmap.currentStageOrder()).isNull();
        assertThat(roadmap.explanations()).hasSize(1);
        assertThat(roadmap.explanations().get(0)).contains("No destination is available");
    }

    @Test
    void rm005ADebtFreedomGoalAloneCreatesNoStage() {
        RoadmapInput.GoalSnapshot debtFreedom = goal(EMERGENCY_ID, "Debt free",
                "DEBT_FREEDOM", null, null, null, 1, "ACTIVE", CREATED);

        Roadmap roadmap = generate(noDebt(), debtFreedom);

        assertThat(roadmap.routeStatus()).isEqualTo(RouteStatus.NO_DESTINATION);
        assertThat(roadmap.stages()).isEmpty();
    }

    @Test
    void rm004EveryReachedStageMakesTheRouteCompleted() {
        Roadmap roadmap = generate(noDebt(),
                completedGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "2027-10-22"),
                completedGoal(SAVINGS_ID, "Savings", "SAVINGS", "90000000.00", "2027-12-31"));

        assertThat(roadmap.routeStatus()).isEqualTo(RouteStatus.COMPLETED);
        assertThat(roadmap.currentStageOrder()).isNull();
        assertThat(roadmap.stages()).extracting(RoadmapStage::status)
                .containsExactly(StageStatus.COMPLETED, StageStatus.COMPLETED);
        assertThat(roadmap.explanations())
                .anySatisfy(line -> assertThat(line).contains("Every stage has reached"));
    }

    @Test
    void rm012ArchivedAndDebtFreedomGoalsNeverBecomeStages() {
        RoadmapInput.GoalSnapshot archived = goal(SAVINGS_ID, "Old savings", "SAVINGS",
                "1000000.00", "0.00", null, 1, "ARCHIVED", CREATED);

        Roadmap roadmap = generate(noDebt(), archived);

        assertThat(roadmap.stages()).isEmpty();
        assertThat(roadmap.routeStatus()).isEqualTo(RouteStatus.NO_DESTINATION);
    }

    // ---------------------------------------------------------------- RM-010 / RM-011: Goal.ORDERING (priority, createdAt, id)

    @Test
    void rm010Rm011GoalOrderingKeepsPriorityThenCreatedAtNullsFirstThenId() {
        UUID earlierId = UUID.fromString("00000000-0000-4000-8000-000000000000");
        UUID laterId = UUID.fromString("99999999-9999-4999-8999-999999999999");
        RoadmapInput.GoalSnapshot noCreatedAt = goal(HOUSING_ID, "Housing", "HOUSING",
                "1000000.00", "0.00", null, 5, "ACTIVE", null);
        // Undated too, with a smaller id: the id is the last tie-break among equal priorities.
        RoadmapInput.GoalSnapshot noCreatedAtLowerId = goal(earlierId, "Car", "OTHER",
                "1000000.00", "0.00", null, 5, "ACTIVE", null);
        RoadmapInput.GoalSnapshot createdFirst = goal(EDUCATION_ID, "Education", "EDUCATION",
                "1000000.00", "0.00", null, 5, "ACTIVE", CREATED);
        RoadmapInput.GoalSnapshot createdSecond = goal(laterId, "Later", "OTHER",
                "1000000.00", "0.00", null, 5, "ACTIVE", CREATED.plusSeconds(60));
        RoadmapInput.GoalSnapshot higherPriority = goal(SAVINGS_ID, "Savings", "SAVINGS",
                "1000000.00", "0.00", null, 1, "ACTIVE", CREATED);

        Roadmap roadmap = generate(noDebt(), createdSecond, createdFirst, noCreatedAt,
                noCreatedAtLowerId, higherPriority);

        assertThat(roadmap.stages()).extracting(RoadmapStage::objective)
                .containsExactly("Savings", "Car", "Housing", "Education", "Later");
    }

    // ---------------------------------------------------------------- RM-013 determinism / RM-016 no allocation data

    @Test
    void rm013IdenticalFactsAndAsOfProduceAnIdenticalRoadmap() {
        RoadmapInput input = input(activeDebt(),
                activeGoal(EMERGENCY_ID, "Emergency fund", "EMERGENCY_FUND", "60000000.00",
                        "10000000.00", "2027-10-22", 1),
                completedGoal(SAVINGS_ID, "Savings", "SAVINGS", "90000000.00", "2027-12-31"));

        assertThat(RoadmapGenerator.generate(input)).isEqualTo(RoadmapGenerator.generate(input));
        assertThat(RoadmapGenerator.generate(input).provenance())
                .isEqualTo(RoadmapGenerator.generate(input).provenance());
    }

    @Test
    void rm016NoStageCarriesAnAllocationStatusFundedAmountOrShortfallAmount() {
        List<String> fields = Arrays.stream(RoadmapStage.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .toList();

        assertThat(fields).noneMatch(field -> field.contains("allocation"));
        assertThat(fields).noneMatch(field -> field.contains("funded"));
        assertThat(fields).noneMatch(field -> field.contains("shortfall"));

        Roadmap roadmap = generate(activeDebt());
        RoadmapStage stage = roadmap.stages().get(0);
        assertThat(stage.status()).isEqualTo(StageStatus.CURRENT);
        assertThat(stage.nextAction()).isEqualTo(
                "Keep paying the planned monthly payment on every debt. The payoff date assumes "
                        + "every planned payment, including any Debt Extra, is made in full.");
    }
}
