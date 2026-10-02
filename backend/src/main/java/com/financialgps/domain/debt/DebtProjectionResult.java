package com.financialgps.domain.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Single-debt payoff projection (spec §5.2–§5.3). Finite payoffs carry a date, payment count,
 * total interest and clamped final payment; BLOCKED projections carry nulls plus a machine-readable
 * reason code and a human explanation (Constitution I + III: never invent a payoff date).
 */
public record DebtProjectionResult(
        ProjectionStatus status,
        LocalDate projectedPayoffDate,
        Integer numberOfPayments,
        BigDecimal totalInterest,
        BigDecimal finalPayment,
        String reasonCode,
        String explanation) {

    public DebtProjectionResult {
        Objects.requireNonNull(status, "status");
    }

    public static DebtProjectionResult available(LocalDate payoffDate,
                                                 int numberOfPayments,
                                                 BigDecimal totalInterest,
                                                 BigDecimal finalPayment) {
        return new DebtProjectionResult(ProjectionStatus.AVAILABLE, payoffDate, numberOfPayments,
                totalInterest, finalPayment, null, null);
    }

    public static DebtProjectionResult completed(LocalDate asOf) {
        return new DebtProjectionResult(ProjectionStatus.COMPLETED, asOf, 0,
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                "DEBT_ALREADY_PAID", "Debt is already fully paid.");
    }

    public static DebtProjectionResult blocked(String reasonCode, String explanation) {
        Objects.requireNonNull(reasonCode, "reasonCode");
        Objects.requireNonNull(explanation, "explanation");
        return new DebtProjectionResult(ProjectionStatus.BLOCKED, null, null, null, null,
                reasonCode, explanation);
    }
}
