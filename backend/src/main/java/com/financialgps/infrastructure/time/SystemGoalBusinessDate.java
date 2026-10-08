package com.financialgps.infrastructure.time;

import com.financialgps.application.goal.port.out.GoalBusinessDate;
import com.financialgps.application.profile.port.out.BusinessDate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * Goal-context adapter for {@link GoalBusinessDate}.
 *
 * <p>It delegates to the single clock adapter ({@link BusinessDate}) instead of reading a wall clock
 * again, so the "one production clock" rule still holds while the goal context keeps its own port.
 */
@Component
class SystemGoalBusinessDate implements GoalBusinessDate {

    private final BusinessDate clock;

    SystemGoalBusinessDate(BusinessDate clock) {
        this.clock = clock;
    }

    @Override
    public LocalDate today() {
        return clock.today();
    }
}
