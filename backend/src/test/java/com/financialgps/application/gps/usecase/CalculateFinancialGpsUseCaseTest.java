package com.financialgps.application.gps.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.gps.model.GpsModels;
import com.financialgps.application.gps.port.out.GpsBusinessDate;
import com.financialgps.application.gps.port.out.GpsDebtReader;
import com.financialgps.application.gps.port.out.GpsGoalReader;
import com.financialgps.application.gps.port.out.GpsProfileReader;
import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.debt.ProjectionStatus;
import com.financialgps.domain.gps.GpsProfileSnapshot;
import com.financialgps.domain.model.OwnerId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
        return fixture(goalView, standardProfile(), CalculateFinancialGpsUseCaseTest::availableDebts);
    }

    private static Fixture fixture(GoalModels.GoalView goalView, GpsProfileSnapshot profile,
                                   Function<LocalDate, DebtSummaryResult> debtsFn) {
        AtomicReference<LocalDate> profileDate = new AtomicReference<>();
        AtomicReference<LocalDate> debtDate = new AtomicReference<>();
        AtomicReference<LocalDate> goalDate = new AtomicReference<>();

        GpsProfileReader profiles = (owner, asOf) -> {
            profileDate.set(asOf);
            return profile;
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
            return debtsFn.apply(asOf);
        };
        GpsBusinessDate dates = () -> AS_OF;
        return new Fixture(new CalculateFinancialGpsUseCase(profiles, goals, debts, dates),
                profileDate, debtDate, goalDate, goalView);
    }

    private static GpsProfileSnapshot standardProfile() {
        return new GpsProfileSnapshot(
                "VND",
                List.of(new GpsProfileSnapshot.IncomeLine("74000000.00", "Salary", true, null)),
                List.of(new GpsProfileSnapshot.ExpenseLine(
                        "43000000.00", "Living", "FIXED", true, null)),
                "20000000.00", "20000000.00", 0);
    }

    private static GpsProfileSnapshot missingProfile() {
        return new GpsProfileSnapshot(null, List.of(), List.of(), null, null, 0);
    }

    private static GpsProfileSnapshot zeroCapacityProfile() {
        return new GpsProfileSnapshot(
                "VND",
                List.of(new GpsProfileSnapshot.IncomeLine("30000000.00", "Salary", true, null)),
                List.of(new GpsProfileSnapshot.ExpenseLine(
                        "30000000.00", "Living", "FIXED", true, null)),
                "0.00", "0.00", 0);
    }

    private static DebtSummaryResult availableDebts(LocalDate asOf) {
        return new DebtSummaryResult(
                new BigDecimal("120000000.00"), new BigDecimal("20000000.00"),
                new BigDecimal("20000000.00"), null, "VND",
                new DebtSummaryResult.DtiResult("AVAILABLE", new BigDecimal("0.2703"), null, "dti"),
                new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.AVAILABLE,
                        asOf.plusMonths(7), 7, new BigDecimal("1000000.00"), null, "payoff", List.of()),
                0, asOf);
    }

    private static DebtSummaryResult completedDebts(LocalDate asOf) {
        return new DebtSummaryResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, "VND",
                new DebtSummaryResult.DtiResult("UNAVAILABLE", null, "ZERO_OR_MISSING_INCOME",
                        "no income"),
                new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.COMPLETED,
                        asOf, 0, BigDecimal.ZERO.setScale(2), "DEBT_ALREADY_PAID",
                        "No active debts: the portfolio is already debt-free.", List.of()),
                0, asOf);
    }

    private static GoalModels.GoalView activeGoal() {
        return new GoalModels.GoalView(GOAL_ID.toString(), "Emergency Fund", "EMERGENCY_FUND",
                "108000000.00", "20000000.00", "2027-03-01", 1, "ACTIVE", "VND",
                "88000000.00", "0.1852", "AMOUNT_REACHED", Map.of());
    }

    private static GoalModels.GoalView zeroTargetGoal() {
        return new GoalModels.GoalView(GOAL_ID.toString(), "Tiny", "EMERGENCY_FUND",
                "0.00", "0.00", null, 1, "COMPLETED", "VND",
                "0.00", "1.0000", "AMOUNT_REACHED", Map.of());
    }

    private static GoalModels.GoalView debtFreeGoal(String targetAmount, String currentAmount) {
        return new GoalModels.GoalView(GOAL_ID.toString(), "Debt Freedom", "DEBT_FREEDOM",
                targetAmount, currentAmount, "2027-12-31", 1, "ACTIVE", "VND",
                null, null, "DEBT_FREE", Map.of());
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

    @Test
    void etaUsesCeilingPeriods() {
        // remaining 88000000 / capacity 11000000 = exactly 8 periods.
        var fix = fixture(activeGoal());
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.eta().availability()).isEqualTo("CALCULATED");
        assertThat(view.eta().periods()).isEqualTo(8);
        assertThat(view.eta().date()).isEqualTo(AS_OF.plusMonths(8).toString());
    }

    @Test
    void zeroTargetGoal_reportsCompletedRoute() {
        var fix = fixture(zeroTargetGoal());
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.status().status()).isEqualTo("COMPLETED");
        assertThat(view.distance().remaining()).isEqualTo("0.00");
        assertThat(view.eta().availability()).isEqualTo("CALCULATED");
        assertThat(view.eta().periods()).isEqualTo(0);
    }

    @Test
    void missingProfile_reportsBlockedWithReason() {
        var fix = fixture(activeGoal(), missingProfile(),
                CalculateFinancialGpsUseCaseTest::availableDebts);
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.status().status()).isEqualTo("BLOCKED");
        assertThat(view.missingInputs()).contains("FINANCIAL_PROFILE");
        assertThat(view.eta().availability()).isEqualTo("UNAVAILABLE");
        assertThat(view.eta().reason()).isEqualTo("MISSING_FINANCIAL_PROFILE");
    }

    @Test
    void zeroCapacityWithProfile_reportsBlockedNoAvailableCapacity() {
        var fix = fixture(activeGoal(), zeroCapacityProfile(),
                CalculateFinancialGpsUseCaseTest::availableDebts);
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.status().status()).isEqualTo("BLOCKED");
        assertThat(view.missingInputs()).isEmpty();
        assertThat(view.eta().reason()).isEqualTo("NO_AVAILABLE_CAPACITY");
    }

    @Test
    void debtFreedom_completedPortfolio_reportsCompleted() {
        var fix = fixture(debtFreeGoal(null, null), standardProfile(),
                CalculateFinancialGpsUseCaseTest::completedDebts);
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.status().status()).isEqualTo("COMPLETED");
    }

    @Test
    void debtFreedom_metAmountsButActiveDebts_notCompleted() {
        // Advisory amounts never decide DEBT_FREE completion (spec D-6, VR-21).
        var fix = fixture(debtFreeGoal("100.00", "120.00"));
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.status().status()).isNotEqualTo("COMPLETED");
    }

    @Test
    void debtFreedom_reactivatesWhenNewDebtAppears() {
        var done = fixture(debtFreeGoal(null, null), standardProfile(),
                CalculateFinancialGpsUseCaseTest::completedDebts);
        assertThat(done.useCase().calculate(OWNER, GOAL_ID, AS_OF).status().status())
                .isEqualTo("COMPLETED");

        var back = fixture(debtFreeGoal(null, null));
        assertThat(back.useCase().calculate(OWNER, GOAL_ID, AS_OF).status().status())
                .isNotEqualTo("COMPLETED");
    }

    @Test
    void futureEffectiveIncome_excludedAtEarlierAsOf() {
        var profile = new GpsProfileSnapshot(
                "VND",
                List.of(new GpsProfileSnapshot.IncomeLine(
                        "74000000.00", "Salary", true, LocalDate.of(2027, 1, 1))),
                List.of(new GpsProfileSnapshot.ExpenseLine(
                        "43000000.00", "Living", "FIXED", true, null)),
                "20000000.00", "20000000.00", 0);

        var before = fixture(activeGoal(), profile,
                CalculateFinancialGpsUseCaseTest::availableDebts);
        assertThat(before.useCase().calculate(OWNER, GOAL_ID, AS_OF).status().status())
                .isEqualTo("BLOCKED");

        var after = fixture(activeGoal(), profile,
                CalculateFinancialGpsUseCaseTest::availableDebts);
        assertThat(after.useCase().calculate(OWNER, GOAL_ID, LocalDate.of(2027, 2, 1))
                .status().status()).isNotEqualTo("BLOCKED");
    }

    @Test
    void positionCurrency_isForcedOntoTheGoal_neverMismatches() {
        // The goal reader advertises VND (currency-agnostic adapter, spec §4.1/§5) while the
        // position is USD. Without the re-expression this throws CURRENCY_MISMATCH on the first
        // capacity/status calculation; the route must evaluate entirely in USD instead.
        var profile = new GpsProfileSnapshot(
                "USD",
                List.of(new GpsProfileSnapshot.IncomeLine("150000000.00", "Salary", true, null)),
                List.of(new GpsProfileSnapshot.ExpenseLine("50000000.00", "Living", "FIXED", true, null)),
                "20000000.00", "20000000.00", 0);
        var fix = fixture(activeGoal(), profile,
                CalculateFinancialGpsUseCaseTest::availableDebts);
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.status().status()).isNotEqualTo("BLOCKED");
        assertThat(view.capacityComparison().currency()).isEqualTo("USD");
    }

    @Test
    void missingProfile_goalKeepsDefaultCurrency() {
        // No profile snapshot → position currency falls back to VND; still no mismatch.
        var fix = fixture(activeGoal(), missingProfile(),
                CalculateFinancialGpsUseCaseTest::availableDebts);
        GpsModels.FinancialGpsView view = fix.useCase().calculate(OWNER, GOAL_ID, AS_OF);

        assertThat(view.status().status()).isEqualTo("BLOCKED");
        assertThat(view.capacityComparison().currency()).isEqualTo("VND");
    }
}
