package com.financialgps.infrastructure.time;

import com.financialgps.application.goal.port.out.GoalBusinessDate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/** System-clock adapter for the goal business date. */
@Component
class SystemGoalBusinessDate implements GoalBusinessDate {

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
