package com.financialgps.api.profile;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HTTP adapter DTOs. Decimal strings only; no ownerId/accountId/profileId fields. Validation here
 * is the presentation boundary (400 VALIDATION_FAILED); the domain still owns financial rules.
 */
public final class ProfileDtos {

    /** ISO 4217 code, matching the CHAR(3) column. */
    private static final String CURRENCY = "^[A-Z]{3}$";

    /** Plain decimal string with at most two fraction digits (calculation-rules §1). */
    private static final String MONEY = "^\\d+(\\.\\d{1,2})?$";

    /** numeric(19,2): at most 17 digits before the decimal point, else the column overflows. */
    private static final int MONEY_INTEGER_DIGITS = 17;

    private ProfileDtos() {
    }

    public record PutProfileRequest(
            @NotBlank @Pattern(regexp = CURRENCY, message = "must be a 3-letter currency code") String currency,
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount") @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String savingsAmount,
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount") @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String emergencyFundAmount,
            @PositiveOrZero int dependentsCount) {
    }

    public record IncomeRequest(
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount") @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String amount,
            @NotBlank String source) {
    }

    public record ExpenseRequest(
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount") @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String amount,
            @NotBlank String category,
            @NotBlank @Pattern(regexp = "^(FIXED|VARIABLE)$", message = "must be FIXED or VARIABLE") String expenseType) {
    }
}
