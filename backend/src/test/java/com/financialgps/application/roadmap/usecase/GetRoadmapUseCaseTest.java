package com.financialgps.application.roadmap.usecase;

import com.financialgps.application.roadmap.model.RoadmapModels;
import com.financialgps.application.roadmap.port.out.RoadmapBusinessDate;
import com.financialgps.application.roadmap.port.out.RoadmapDebtReader;
import com.financialgps.application.roadmap.port.out.RoadmapGoalReader;
import com.financialgps.application.roadmap.port.out.RoadmapProfileReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalType;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.RoadmapInput;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T012 — US1 orchestration: owner scoping, explicit {@code asOf} propagation, business-date
 * fallback and determinism. Financial rules live in the domain; this only pins the wiring.
 */
class GetRoadmapUseCaseTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 22);
    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());
    private static final UUID GOAL_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    private record Fixture(GetRoadmapUseCase useCase,
                           AtomicReference<LocalDate> profileDate,
                           AtomicReference<LocalDate> debtDate,
                           AtomicReference<OwnerId> goalOwner) {
    }

    private static RoadmapInput.GoalSnapshot goal() {
        return new RoadmapInput.GoalSnapshot(
                GOAL_ID,
                "Emergency fund",
                GoalType.EMERGENCY_FUND.name(),
                Goal.COMPLETION_AMOUNT_REACHED,
                "60000000.00",
                "10000000.00",
                "2027-10-22",
                1,
                "ACTIVE",
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static Fixture fixture(RoadmapInput.ProfileSnapshot profile,
                                   RoadmapInput.DebtPortfolioFacts debts,
                                   List<RoadmapInput.GoalSnapshot> goals) {
        AtomicReference<LocalDate> profileDate = new AtomicReference<>();
        AtomicReference<LocalDate> debtDate = new AtomicReference<>();
        AtomicReference<OwnerId> goalOwner = new AtomicReference<>();

        RoadmapProfileReader profiles = (owner, asOf) -> {
            assertThat(owner).isEqualTo(OWNER);
            profileDate.set(asOf);
            return profile;
        };
        RoadmapDebtReader debtReader = (owner, asOf) -> {
            assertThat(owner).isEqualTo(OWNER);
            debtDate.set(asOf);
            return debts;
        };
        RoadmapGoalReader goalReader = owner -> {
            goalOwner.set(owner);
            return goals;
        };
        RoadmapBusinessDate dates = () -> AS_OF;

        return new Fixture(new GetRoadmapUseCase(profiles, debtReader, goalReader, dates),
                profileDate, debtDate, goalOwner);
    }

    private static RoadmapInput.DebtPortfolioFacts debt(String status) {
        return new RoadmapInput.DebtPortfolioFacts(status, "100000000.00", "5000000.00",
                "7000000.00", "AVAILABLE".equals(status) ? "2027-04-30" : null,
                "AVAILABLE".equals(status) ? 18 : null,
                "AVAILABLE".equals(status) ? null : "DEBT_ALREADY_PAID",
                "Projection explanation.", List.of());
    }

    @Test
    void explicitAsOfIsPropagatedToEveryReaderAndEchoedInTheView() {
        Fixture fixture = fixture(new RoadmapInput.ProfileSnapshot("VND", "31000000.00"),
                debt("AVAILABLE"), List.of(goal()));

        RoadmapModels.RoadmapView view = fixture.useCase().roadmap(OWNER, AS_OF);

        assertThat(fixture.profileDate().get()).isEqualTo(AS_OF);
        assertThat(fixture.debtDate().get()).isEqualTo(AS_OF);
        assertThat(fixture.goalOwner().get()).isEqualTo(OWNER);
        assertThat(view.asOf()).isEqualTo(AS_OF.toString());
        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.routeStatus()).isEqualTo("IN_PROGRESS");
        assertThat(view.currentStageOrder()).isEqualTo(1);
        assertThat(view.stages()).hasSize(2);
        assertThat(view.orderPolicy().scope()).isEqualTo("ROADMAP_STAGE_ORDER");
    }

    @Test
    void omittedAsOfIsResolvedByTheBusinessDatePort() {
        Fixture fixture = fixture(new RoadmapInput.ProfileSnapshot("USD", "1000000.00"),
                debt("COMPLETED"), List.of(goal()));

        RoadmapModels.RoadmapView view = fixture.useCase().roadmap(OWNER);

        assertThat(fixture.profileDate().get()).isEqualTo(AS_OF);
        assertThat(fixture.debtDate().get()).isEqualTo(AS_OF);
        assertThat(view.asOf()).isEqualTo(AS_OF.toString());
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.routeStatus()).isEqualTo("IN_PROGRESS");
        assertThat(view.currentStageOrder()).isEqualTo(1);
    }

    @Test
    void theRouteIsAProjectionAndTwoReadsOfTheSameFactsAreIdentical() {
        Fixture fixture = fixture(new RoadmapInput.ProfileSnapshot("VND", "31000000.00"),
                debt("AVAILABLE"), List.of(goal()));

        RoadmapModels.RoadmapView first = fixture.useCase().roadmap(OWNER, AS_OF);
        RoadmapModels.RoadmapView second = fixture.useCase().roadmap(OWNER, AS_OF);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void anEmptyFactSetIsNoDestination() {
        Fixture fixture = fixture(new RoadmapInput.ProfileSnapshot("VND", null),
                debt("COMPLETED"), List.of());

        RoadmapModels.RoadmapView view = fixture.useCase().roadmap(OWNER, AS_OF);

        assertThat(view.routeStatus()).isEqualTo("NO_DESTINATION");
        assertThat(view.stages()).isEmpty();
        assertThat(view.currentStageOrder()).isNull();
        assertThat(view.explanations()).isNotEmpty();
    }
}
