package com.financialgps.application.goal.model;

import java.util.List;
import java.util.Map;

/**
 * Goal application models: commands in, typed views out. Money travels as plain decimal strings;
 * derived values state their provenance (actual vs calculated).
 */
public final class GoalModels {

    private GoalModels() {
    }

    public record GoalCommand(String name, String goalType, String targetAmount,
                              String currentAmount, String targetDate, Integer priority) {
    }

    public record GoalUpdateCommand(String name, String goalType, String targetAmount,
                                    String currentAmount, String targetDate, Integer priority) {
    }

    public record GoalView(String id, String name, String goalType, String targetAmount,
                           String currentAmount, String targetDate, int priority, String status,
                           String currency, String remaining, String progress,
                           String completionCondition, Map<String, String> derived) {
    }

    public record GoalCapacityView(String goalId, String asOf, String remaining,
                                   Integer monthsRemaining, String requiredMonthlyCapacity,
                                   String availableCapacity, String capacityCoverage,
                                   String monthlyShortfall, String dateFeasibility,
                                   String explanation) {
    }
}
