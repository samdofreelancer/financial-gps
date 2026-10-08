package com.financialgps.domain.goal;

import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T001: Goal aggregate invariants (spec §4.1, §4.5, §8). */
class GoalDomainValidationTest {

    private static Goal goal(String target, String current) {
        return Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND", target, current,
                LocalDate.of(2027, 12, 31), 1);
    }

    @Test
    void rejectsNegativeTarget() {
        assertThatThrownBy(() -> goal("-1.00", "0.00"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsNegativeCurrent() {
        assertThatThrownBy(() -> goal("100.00", "-5.00"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsPriorityBelowOne_defaultsToOne() {
        assertThatThrownBy(() -> goalWithPriority(0))
                .isInstanceOf(DomainValidationException.class);
        assertThat(goalWithPriority(null).priority()).isEqualTo(1);
    }

    private static Goal goalWithPriority(Integer priority) {
        return Goal.recorded("VND", "House", "HOUSING", "100.00", "10.00", null, priority);
    }

    @Test
    void amountReachedCompletionTransitionsActiveToCompleted() {
        assertThat(goal("120000000.00", "30000000.00").status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal("120000000.00", "120000000.00").status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(goal("0.00", "0.00").status()).isEqualTo(GoalStatus.COMPLETED);
    }

    @Test
    void completionReevaluatedOnEveryMutation() {
        Goal active = goal("120000000.00", "30000000.00");
        Goal completed = active.withProgress(Money.of("120000000.00", "VND"),
                Money.of("120000000.00", "VND"));
        assertThat(completed.status()).isEqualTo(GoalStatus.COMPLETED);
        Goal reopened = completed.withProgress(Money.of("120000000.00", "VND"),
                Money.of("10000000.00", "VND"));
        assertThat(reopened.status()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void archiveIsTerminal() {
        Goal archived = goal("120000000.00", "30000000.00").archived();
        assertThat(archived.status()).isEqualTo(GoalStatus.ARCHIVED);
        assertThat(archived.withProgress(Money.of("120000000.00", "VND"),
                Money.of("120000000.00", "VND")).status()).isEqualTo(GoalStatus.ARCHIVED);
    }

    @Test
    void orderingKeyIsDeterministicAndTotal() {
        Instant base = Instant.parse("2026-10-01T00:00:00Z");
        Goal p2 = Goal.reconstitute(GoalId.of(UUID.randomUUID()), base, "VND", "B", "SAVINGS",
                "10.00", "0.00", null, 2, "AMOUNT_REACHED", GoalStatus.ACTIVE);
        Goal p1late = Goal.reconstitute(GoalId.of(UUID.randomUUID()), base.plusSeconds(60), "VND",
                "A", "SAVINGS", "10.00", "0.00", null, 1, "AMOUNT_REACHED", GoalStatus.ACTIVE);
        Goal p1early = Goal.reconstitute(GoalId.of(UUID.randomUUID()), base, "VND", "C", "SAVINGS",
                "10.00", "0.00", null, 1, "AMOUNT_REACHED", GoalStatus.ACTIVE);
        List<Goal> sorted = new ArrayList<>(List.of(p2, p1late, p1early));
        sorted.sort(Goal.ORDERING);
        assertThat(sorted).containsExactly(p1early, p1late, p2);
    }

    @Test
    void rejectsUnknownGoalType() {
        assertThatThrownBy(() -> Goal.recorded("VND", "X", "NOPE", "10.00", "0.00", null, 1))
                .isInstanceOf(DomainValidationException.class);
    }
}
