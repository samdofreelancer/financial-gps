package com.financialgps.api.goal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** HTTP adapter DTOs: decimal strings only, no ownerId. Bean validation → 400 VALIDATION_FAILED. */
public final class GoalDtos {

    private GoalDtos() {
    }

    private static final String MONEY = "^\\d+(\\.\\d{1,2})?$";
    private static final int MONEY_INTEGER_DIGITS = 17;

    public record GoalRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^(DEBT_FREEDOM|EMERGENCY_FUND|SAVINGS|HOUSING|EDUCATION|RETIREMENT|OTHER)$",
                    message = "must be a known goal type") String goalType,
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount")
            @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String targetAmount,
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount")
            @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String currentAmount,
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be ISO-8601 date") String targetDate,
            @Min(value = 1, message = "must be at least 1") Integer priority) {
    }
}
