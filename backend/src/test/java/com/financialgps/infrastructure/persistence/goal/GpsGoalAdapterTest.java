package com.financialgps.infrastructure.persistence.goal;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 004: the GPS goal reader follows the Feature 003 progress rules (via
 * GoalProgressCalculator) instead of dividing inline — zero targets must not throw.
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

    @Test
    void zeroTarget_doesNotThrow_andReportsCompleted() {
        var store = new MapGoalStore();
        GoalId id = GoalId.of(UUID.randomUUID());
        store.put(id, Goal.recorded("VND", "Tiny", "EMERGENCY_FUND", "0.00", "0.00", null, 1)
                .withId(id));
        var adapter = new GpsGoalAdapter(store);

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
        var adapter = new GpsGoalAdapter(store);

        GoalModels.GoalView view =
                adapter.getGoal(new OwnerId(UUID.randomUUID()), id.value(), AS_OF);

        assertThat(view.remaining()).isEqualTo("88000000.00");
        assertThat(view.progress()).isEqualTo("0.1852");
    }

    @Test
    void unknownGoal_returnsNull() {
        var adapter = new GpsGoalAdapter(new MapGoalStore());
        assertThat(adapter.getGoal(new OwnerId(UUID.randomUUID()), UUID.randomUUID(), AS_OF)).isNull();
    }
}
