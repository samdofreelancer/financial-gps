package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.model.OwnerId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 004: the GPS goal reader follows the Feature 003 progress rules (via
 * GoalProgressCalculator) instead of dividing inline — zero targets must not throw —
 * and reconciles {@code DEBT_FREE} goals through the shared Feature 003 lifecycle
 * (dynamic 002-portfolio completion, never a sticky stored flag).
 */
class GpsGoalAdapterTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 11);

    private static final class MapGoalStore implements GoalStore {
        private final Map<String, Goal> goals = new ConcurrentHashMap<>();

        void put(GoalId id, Goal goal) {
            goals.put(id.value().toString(), goal);
        }

        @Override
        public List<Goal> findAllByOwner(OwnerId owner) {
            return List.copyOf(goals.values());
        }

        @Override
        public Optional<Goal> findByIdAndOwner(GoalId id, OwnerId owner) {
            return Optional.ofNullable(goals.get(id.value().toString()));
        }

        @Override
        public Goal save(OwnerId owner, Goal goal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean archiveByIdAndOwner(GoalId id, OwnerId owner) {
            throw new UnsupportedOperationException();
        }
    }

    /** Mutable 002 summary stub: swap the held view to simulate portfolio changes. */
    private static final class StubDebts implements GetDebtSummary {
        private final AtomicReference<DebtModels.DebtSummaryView> current;

        StubDebts(DebtModels.DebtSummaryView initial) {
            this.current = new AtomicReference<>(initial);
        }

        void set(DebtModels.DebtSummaryView next) {
            current.set(next);
        }

        @Override
        public DebtModels.DebtSummaryView summary(OwnerId owner) {
            return current.get();
        }

        @Override
        public DebtModels.DebtSummaryView summary(OwnerId owner, LocalDate asOfDate) {
            return current.get();
        }
    }

    private static DebtModels.DebtSummaryView summaryWithPortfolioStatus(String status) {
        return new DebtModels.DebtSummaryView(
                "120000000.00", "20000000.00", "20000000.00", "1000000.00", "VND",
                new DebtModels.DtiView("AVAILABLE", "0.2703", null, "dti"),
                new DebtModels.PortfolioProjectionView(status, "2027-09-01", 11,
                        "1000000.00", null, "projection", List.of()),
                0, AS_OF.toString());
    }

    private static DebtModels.DebtSummaryView completedPortfolio() {
        return new DebtModels.DebtSummaryView(
                "0.00", "0.00", "0.00", "0.00", "VND",
                new DebtModels.DtiView("UNAVAILABLE", null, "ZERO_OR_MISSING_INCOME", "no income"),
                new DebtModels.PortfolioProjectionView("COMPLETED", null, 0,
                        "0.00", "DEBT_ALREADY_PAID", "No active debts.", List.of()),
                0, AS_OF.toString());
    }

    private static GoalId putDebtFreeGoal(MapGoalStore store) {
        GoalId id = GoalId.of(UUID.randomUUID());
        // Advisory amounts meet the target (120 >= 100) — completion must still follow 002.
        store.put(id, Goal.recorded("VND", "Debt Freedom", "DEBT_FREEDOM",
                "100.00", "120.00", LocalDate.of(2027, 12, 31), 1).withId(id));
        return id;
    }

    @Test
    void zeroTarget_doesNotThrow_andReportsCompleted() {
        var store = new MapGoalStore();
        GoalId id = GoalId.of(UUID.randomUUID());
        store.put(id, Goal.recorded("VND", "Tiny", "EMERGENCY_FUND", "0.00", "0.00", null, 1)
                .withId(id));
        var adapter = new GpsGoalAdapter(store, new StubDebts(summaryWithPortfolioStatus("AVAILABLE")));

        // Would throw ArithmeticException before the 003-rules fix; a throw fails the test.
        GoalModels.GoalView view =
                adapter.getGoal(new OwnerId(UUID.randomUUID()), id.value(), AS_OF);

        assertThat(view.remaining()).isEqualTo("0.00");
        assertThat(view.progress()).isEqualTo("1.0000");
        // 003 reports the evaluated lifecycle, not the stored flag.
        assertThat(view.status()).isEqualTo("COMPLETED");
    }

    @Test
    void amountGoal_follows003Progress() {
        var store = new MapGoalStore();
        GoalId id = GoalId.of(UUID.randomUUID());
        store.put(id, Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND",
                "108000000.00", "20000000.00", LocalDate.of(2027, 3, 1), 1).withId(id));
        var adapter = new GpsGoalAdapter(store, new StubDebts(summaryWithPortfolioStatus("AVAILABLE")));

        GoalModels.GoalView view =
                adapter.getGoal(new OwnerId(UUID.randomUUID()), id.value(), AS_OF);

        assertThat(view.remaining()).isEqualTo("88000000.00");
        assertThat(view.progress()).isEqualTo("0.1852");
    }

    @Test
    void debtFreeGoal_activeDebts_staysActive_despiteAdvisoryAmountsMet() {
        // REF-G09: advisory currentAmount >= targetAmount but ACTIVE debts remain → ACTIVE,
        // with remaining/progress not applicable (distance lives in the 002 portfolio).
        var store = new MapGoalStore();
        GoalId id = putDebtFreeGoal(store);
        var adapter = new GpsGoalAdapter(store, new StubDebts(summaryWithPortfolioStatus("AVAILABLE")));

        GoalModels.GoalView view =
                adapter.getGoal(new OwnerId(UUID.randomUUID()), id.value(), AS_OF);

        assertThat(view.completionCondition()).isEqualTo("DEBT_FREE");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.remaining()).isNull();
        assertThat(view.progress()).isNull();
    }

    @Test
    void debtFreeGoal_completedPortfolio_reportsCompleted() {
        // VR-22 / REF-G10: no ACTIVE debts remain → COMPLETED, independent of advisory amounts.
        var store = new MapGoalStore();
        GoalId id = putDebtFreeGoal(store);
        var adapter = new GpsGoalAdapter(store, new StubDebts(completedPortfolio()));

        GoalModels.GoalView view =
                adapter.getGoal(new OwnerId(UUID.randomUUID()), id.value(), AS_OF);

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.remaining()).isNull();
        assertThat(view.progress()).isNull();
    }

    @Test
    void debtFreeGoal_reactivates_whenNewActiveDebtAppears() {
        // VR-24 / REF-G15 / GC-005: completion is dynamic, not sticky — a new ACTIVE debt
        // returns the goal to ACTIVE instead of preserving a stale COMPLETED flag.
        var store = new MapGoalStore();
        GoalId id = putDebtFreeGoal(store);
        var debts = new StubDebts(completedPortfolio());
        var adapter = new GpsGoalAdapter(store, debts);
        OwnerId owner = new OwnerId(UUID.randomUUID());

        assertThat(adapter.getGoal(owner, id.value(), AS_OF).status()).isEqualTo("COMPLETED");

        debts.set(summaryWithPortfolioStatus("AVAILABLE"));

        GoalModels.GoalView reactivated = adapter.getGoal(owner, id.value(), AS_OF);
        assertThat(reactivated.status()).isEqualTo("ACTIVE");
        assertThat(reactivated.remaining()).isNull();
        assertThat(reactivated.progress()).isNull();
    }

    @Test
    void unknownGoal_returnsNull() {
        var adapter = new GpsGoalAdapter(new MapGoalStore(),
                new StubDebts(summaryWithPortfolioStatus("AVAILABLE")));
        assertThat(adapter.getGoal(new OwnerId(UUID.randomUUID()), UUID.randomUUID(), AS_OF)).isNull();
    }
}
