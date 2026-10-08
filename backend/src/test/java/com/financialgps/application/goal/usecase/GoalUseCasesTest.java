package com.financialgps.application.goal.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStatus;
import com.financialgps.domain.goal.GoalStore;
import com.financialgps.domain.model.OwnerId;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T006: goal use cases over mocked output ports (no Spring, no DB). */
class GoalUseCasesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());

    private final GoalStore goals = mock(GoalStore.class);
    private final GoalPositionReader positions = mock(GoalPositionReader.class);
    private final GoalBusinessDate dates = () -> TODAY;
    private final CreateGoalUseCase create = new CreateGoalUseCase(goals, positions, dates);
    private final UpdateGoalUseCase update = new UpdateGoalUseCase(goals, positions, dates);
    private final DeleteGoalUseCase delete = new DeleteGoalUseCase(goals);
    private final GetGoalsUseCase getGoals = new GetGoalsUseCase(goals, positions, dates);
    private final GetGoalCapacityUseCase capacity = new GetGoalCapacityUseCase(goals, positions);

    private static GoalPositionReader.PositionSnapshot snapshot(String amount, String currency) {
        return new GoalPositionReader.PositionSnapshot(amount, currency, TODAY);
    }

    private static GoalModels.GoalCommand command(String target, String current) {
        return new GoalModels.GoalCommand("Emergency Fund", "EMERGENCY_FUND", target, current,
                "2027-12-31", 1);
    }

    private static Goal stored(UUID id, String target, String current) {
        return Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND", target, current,
                LocalDate.of(2027, 12, 31), 1).withId(GoalId.of(id));
    }

    @Test
    void createAssignsServerIdAndLabelsProvenance() {
        when(positions.snapshot(OWNER)).thenReturn(snapshot("24000000.00", "VND"));
        when(goals.save(any(), any())).thenAnswer(i -> {
            Goal g = i.getArgument(1);
            return g.id() == null ? g.withId(GoalId.of(UUID.randomUUID())) : g;
        });

        GoalModels.GoalView view = create.create(OWNER, command("120000000.00", "30000000.00"));

        assertThat(view.id()).isNotBlank();
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.remaining()).isEqualTo("90000000.00");
        assertThat(view.progress()).isEqualTo("0.2500");
        assertThat(view.derived()).containsEntry("remaining", "calculated")
                .containsEntry("currentAmount", "actual");
    }

    @Test
    void createDerivesCurrencyFromPosition() {
        when(positions.snapshot(OWNER)).thenReturn(snapshot("0.00", "USD"));
        when(goals.save(any(), any())).thenAnswer(i -> {
            Goal g = i.getArgument(1);
            return g.id() == null ? g.withId(GoalId.of(UUID.randomUUID())) : g;
        });

        assertThat(create.create(OWNER, command("100.00", "10.00")).currency()).isEqualTo("USD");
    }

    @Test
    void updateReevaluatesCompletionOnEveryWrite() {
        UUID id = UUID.randomUUID();
        when(goals.findByIdAndOwner(GoalId.of(id), OWNER))
                .thenReturn(Optional.of(stored(id, "120000000.00", "30000000.00")));
        when(positions.snapshot(OWNER)).thenReturn(snapshot("24000000.00", "VND"));
        when(goals.save(any(), any())).thenAnswer(i -> i.getArgument(1));

        GoalModels.GoalView view = update.update(OWNER, id, new GoalModels.GoalUpdateCommand(
                "Emergency Fund", "EMERGENCY_FUND", "120000000.00", "120000000.00",
                "2027-12-31", 1));

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.remaining()).isEqualTo("0.00");
    }

    @Test
    void missingGoalReadsAs404() {
        UUID id = UUID.randomUUID();
        when(goals.findByIdAndOwner(GoalId.of(id), OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getGoals.get(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> update.update(OWNER, id, new GoalModels.GoalUpdateCommand(
                "X", "SAVINGS", "1.00", "0.00", null, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> delete.delete(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> capacity.capacity(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lostArchiveRaceReadsAs404() {
        UUID id = UUID.randomUUID();
        when(goals.findByIdAndOwner(GoalId.of(id), OWNER))
                .thenReturn(Optional.of(stored(id, "100.00", "10.00")));
        when(goals.archiveByIdAndOwner(GoalId.of(id), OWNER)).thenReturn(false);

        assertThatThrownBy(() -> delete.delete(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listExcludesNothingButArchivedAndKeepsPriorityOrder() {
        Goal low = stored(UUID.randomUUID(), "100.00", "10.00");
        when(goals.findAllByOwner(OWNER)).thenReturn(List.of(low));
        when(positions.snapshot(OWNER)).thenReturn(snapshot("24000000.00", "VND"));

        List<GoalModels.GoalView> views = getGoals.list(OWNER);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    void capacityReadsAvailableCapacityFromPositionReader() {
        UUID id = UUID.randomUUID();
        when(goals.findByIdAndOwner(GoalId.of(id), OWNER))
                .thenReturn(Optional.of(stored(id, "120000000.00", "30000000.00")));
        // 90M over 14 periods → 6428571.43/month against 1M available: shortfall 5428571.43.
        when(positions.snapshot(OWNER)).thenReturn(snapshot("1000000.00", "VND"));

        GoalModels.GoalCapacityView view = capacity.capacity(OWNER, id);

        verify(positions).snapshot(OWNER);
        assertThat(view.availableCapacity()).isEqualTo("1000000.00");
        assertThat(view.capacityCoverage()).isEqualTo("SHORTFALL");
        assertThat(view.monthlyShortfall()).isEqualTo("5428571.43");
    }

    @Test
    void invalidDateFailsValidation() {
        when(positions.snapshot(OWNER)).thenReturn(snapshot("0.00", "VND"));

        assertThatThrownBy(() -> create.create(OWNER, new GoalModels.GoalCommand(
                        "X", "SAVINGS", "100.00", "0.00", "2026-13-99", 1)))
                .isInstanceOf(GoalValidationException.class)
                .extracting(e -> ((GoalValidationException) e).code())
                .isEqualTo("GOAL_DATE_INVALID");
    }

    @Test
    void archivedGoalStaysTerminalInViews() {
        UUID id = UUID.randomUUID();
        Goal archived = stored(id, "100.00", "10.00").archived();
        assertThat(archived.status()).isEqualTo(GoalStatus.ARCHIVED);
        when(goals.findByIdAndOwner(GoalId.of(id), OWNER)).thenReturn(Optional.of(archived));
        when(positions.snapshot(OWNER)).thenReturn(snapshot("24000000.00", "VND"));

        assertThat(getGoals.get(OWNER, id).status()).isEqualTo("ARCHIVED");
    }
}
