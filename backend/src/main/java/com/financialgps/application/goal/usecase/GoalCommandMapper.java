package com.financialgps.application.goal.usecase;

import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.model.DomainValidationException;
import java.time.LocalDate;

/**
 * Shared user-input → domain mapping for goal writes. Currency is position-derived (spec
 * §4.1, §5) and passed in — this mapper never hardcodes one.
 */
final class GoalCommandMapper {

    private GoalCommandMapper() {
    }

    static Goal toDomain(String currency, String name, String goalType, String targetAmount,
                         String currentAmount, String targetDate, Integer priority) {
        try {
            if (name == null || goalType == null || targetAmount == null || currentAmount == null
                    || currency == null) {
                throw new GoalValidationException("GOAL_REQUIRED",
                        "Name, goal type, target amount and current amount are required");
            }
            LocalDate date = targetDate == null || targetDate.isBlank() ? null
                    : LocalDate.parse(targetDate);
            return Goal.recorded(currency, name, goalType, targetAmount, currentAmount,
                    date, priority);
        } catch (GoalValidationException e) {
            throw e;
        } catch (DomainValidationException e) {
            throw new GoalValidationException(e.code(), e.getMessage());
        } catch (java.time.DateTimeException e) {
            throw new GoalValidationException("GOAL_DATE_INVALID",
                    "Target date must be a valid ISO-8601 calendar date");
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new GoalValidationException("GOAL_INVALID_TYPE", "Unknown goal type: " + goalType);
        }
    }
}
