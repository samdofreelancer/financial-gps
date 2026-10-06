package com.financialgps.api.debt;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** HTTP adapter DTOs: decimal strings only, no ownerId. Bean validation → 400 VALIDATION_FAILED. */
public final class DebtDtos {

    private DebtDtos() {
    }

    private static final String MONEY = "^\\d+(\\.\\d{1,2})?$";
    private static final String RATE = "^\\d+(\\.\\d{1,6})?$";
    private static final int MONEY_INTEGER_DIGITS = 17;

    public record DebtRequest(
            @NotBlank @Size(max = 200) String creditor,
            @NotBlank @Pattern(regexp = "^(CREDIT_CARD|MORTGAGE|AUTO_LOAN|STUDENT_LOAN|PERSONAL_LOAN|OTHER)$",
                    message = "must be a known debt type") String debtType,
            // Optional (spec §4.1): omitted/blank means the origination amount is unknown. Never
            // defaulted to 0 by the adapter — the domain keeps "unknown" distinguishable from zero.
            @Pattern(regexp = MONEY, message = "must be a decimal amount")
            @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String originalPrincipal,
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount")
            @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String outstandingBalance,
            @Pattern(regexp = RATE, message = "must be a decimal fraction")
            @Digits(integer = 3, fraction = 6) String annualInterestRate,
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount")
            @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String minimumPayment,
            @NotBlank @Pattern(regexp = MONEY, message = "must be a decimal amount")
            @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2) String plannedPayment,
            Integer dueDay) {
    }
}
