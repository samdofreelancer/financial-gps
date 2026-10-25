package com.financialgps.application.goal.port.out;

import java.time.LocalDate;

/** Technical-time port: the single source of the domain asOf date for goal evaluation. */
public interface GoalBusinessDate {

    LocalDate today();
}
