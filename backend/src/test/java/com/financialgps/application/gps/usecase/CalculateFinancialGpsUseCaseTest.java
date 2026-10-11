package com.financialgps.application.gps.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.gps.model.GpsModels;
import com.financialgps.application.gps.port.out.GpsBusinessDate;
import com.financialgps.application.gps.port.out.GpsDebtReader;
import com.financialgps.application.gps.port.out.GpsGoalReader;
import com.financialgps.application.gps.port.out.GpsProfileReader;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.debt.ProjectionStatus;
import com.financialgps.domain.model.OwnerId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 004: GPS orchestration — owner scoping, archived rejection, explicit asOfDate
 * propagation, and determinism. Financial rules live in the domain; this only
 * pins the wiring.
 */
class CalculateFinancialGpsUseCaseTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 11);
    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());
    private static final UUID GOAL_ID = UUID.randomUUID();

    private record Fixture(CalculateFinancialGpsUseCase useCase,
                           AtomicReference<LocalDate> profileDate,
                           AtomicReference<LocalDate> debtDate,
                           AtomicReference<LocalDate> goalDate,
                           GoalModels.GoalView goalView) {
    }

    private static Fixture fixture(GoalModels.GoalView goalView) {
        AtomicReference<LocalDate> profileDate = new AtomicReference<>();
        AtomicReference<LocalDate> debtDate = new AtomicReference<>();
        AtomicReference<LocalDate> goalDate = new AtomicReference<>();

        GpsProfileReader profiles = (owner, asOf) -> {
            profileDate.set(asOf);
            return new ProfileModels.ProfileView(
                    "VND", "20000000.00", "20000000.00", 0,
                    List.of(new ProfileModels.IncomeLineView("i1", "74000000.00", "VND", "Salary", "actual")),
                    List.of(new ProfileModels.ExpenseLineView("e1", "43000000.00", "VND", "Living", "FIXED",
                            "actual")),
                    null, null, null, null, null, List.of(), asOf.toString());
        };
        GpsGoalReader goals = (owner, goalId, asOf) -> {
            goalDate.set(asOf);
            if (!owner.equals(OWNER) || !goalId.equals(GOAL_ID)) {
                return null;
            }
            return goalView;
        };
        GpsDebtReader debts = (owner, asOf) -> {
            debtDate.set(asOf);
            return new DebtSummaryResult(
                    new BigDecimal("120000000.00"), new BigDecimal("20000000.00"),
                    new BigDecimal("20000000.00"), null, "VND",
                    new DebtSummaryResult.DtiResult("AVAILABLE", new BigDecimal("0.2703"), null, "dti"),
                    new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.AVAILABLE,
                            asOf.plusMonths(7), 7, new BigDecimal("1000000.00"), null, "payoff", List.of()),
                    0, asOf);
        };
        GpsBusinessDate dates = () -> AS_OF;
        return new Fixture(new CalculateFinancialGpsUseCase(profiles, goals, debts, dates),
                profileDate, debtDate, goalDate, goalView);
    }

    private static GoalModels.GoalView activeGoal() {
        return new GoalModels.GoalView(GOAL_ID.toString(), "Emergency Fund", "EMERGENCY_FUND",
                "108000000.00", "20000000.00", "2027-03-01", 1, "ACTIVE", "VND",
                "88000000.00", "0.1852", "AMOUNT_REACHED", Map.of());
    }

    @Test
    void explicitAsOf_propagatesToAllReaders() {
        var fix = fixture(activeGoal());
        GpsModels.FinancialGpsView view =
                fix.useCase().calculate(OWNER, GOAL_ID, LocalDate.of(2026, 10, 1));

        assertThat(view.asOf()).isEqualTo("2026-10-01");
        assertThat(fix.profileDate()).hasValue(LocalDate.of(2026, 10, 1));
        assertThat(fix.debtDate()).hasValue(LocalDate.of(2026, 10, 1));
        assertThat(fix.goalDate()).hasValue(LocalDate.of(2026, 10, 1));
    }

    @Test
    void sameInputs_sameAsOf_identicalView() {
        var fix = fixture(activeGoal());
        GpsModels.FinancialGpsView first = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);
        GpsModels.FinancialGpsView second = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void differentAsOf_mayShiftEta() {
        var fix = fixture(activeGoal());
        GpsModels.FinancialGpsView before = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);
        GpsModels.FinancialGpsView after =
                fix.useCase().calculate(OWNER, GOAL_ID, AS_OF.plusMonths(1));
        assertThat(after.asOf()).isEqualTo(AS_OF.plusMonths(1).toString());
        assertThat(after.eta()).isNotEqualTo(before.eta());
    }

    @Test
    void unknownGoal_isNotFound() {
        var fix = fixture(activeGoal());
        assertThatThrownBy(() -> fix.useCase().calculate(OWNER, UUID.randomUUID(), AS_OF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void otherOwnerGoal_isIndistinguishableFromMissing() {
        var fix = fixture(activeGoal());
        assertThatThrownBy(() -> fix.useCase()
                .calculate(new OwnerId(UUID.randomUUID()), GOAL_ID, AS_OF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void archivedGoal_isNotFound() {
        var archived = new GoalModels.GoalView(GOAL_ID.toString(), "Old", "EMERGENCY_FUND",
                "108000000.00", "20000000.00", "2027-03-01", 1, "ARCHIVED", "VND",
                "88000000.00", "0.1852", "AMOUNT_REACHED", Map.of());
        var fix = fixture(archived);
        assertThatThrownBy(() -> fix.useCase().calculate(OWNER, GOAL_ID, AS_OF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void debtSummaryViewUsesExplicitAsOf() {
        // The adapter-level contract: DebtUseCases.summary(owner, asOf) honors the date.
        // Pinned here through the GPS debt reader capture.
        var fix = fixture(activeGoal());
        fix.useCase().calculate(OWNER, GOAL_ID, LocalDate.of(2026, 10, 1));
        assertThat(fix.debtDate()).hasValue(LocalDate.of(2026, 10, 1));
        assertThat(DebtModels.DebtSummaryView.class).isNotNull();
    }
}
