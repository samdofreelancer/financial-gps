package com.financialgps.application.debt.model;

import java.util.List;

/**
 * Debt application models: commands in, typed results out.
 * Money travels as plain decimal strings; every projection states its reason/explanation.
 */
public final class DebtModels {

    private DebtModels() {
    }

    public record DebtCommand(String creditor, String debtType, String originalPrincipal,
                              String outstandingBalance, String annualInterestRate,
                              String minimumPayment, String plannedPayment, Integer dueDay) {
    }

    public record DebtUpdateCommand(String creditor, String debtType, String originalPrincipal,
                                    String outstandingBalance, String annualInterestRate,
                                    String minimumPayment, String plannedPayment, Integer dueDay) {
    }

    public record ProjectionView(String status, String projectedPayoffDate, Integer numberOfPayments,
                                 String totalInterest, String finalPayment, String monthlyInterest,
                                 String reasonCode, String explanation) {
    }

    public record DebtView(String id, String creditor, String debtType, String originalPrincipal,
                           String outstandingBalance, String annualInterestRate,
                           String minimumPayment, String plannedPayment, Integer dueDay,
                           String status, String currency, ProjectionView projection,
                           boolean paidThisPeriod) {
    }

    public record DtiView(String status, String ratio, String reasonCode, String explanation) {
    }

    public record PortfolioProjectionView(String status, String projectedDebtFreeDate,
                                          Integer totalMonthsRemaining,
                                          String totalInterestRemaining,
                                          String reasonCode, String explanation,
                                          List<BlockedDebtView> blockedDebts) {
    }

    public record BlockedDebtView(String creditor, String reasonCode, String explanation) {
    }

    /** One schedule row: decimal strings, dates ISO-8601 — same transport rules as the views. */
    public record ScheduleRowView(int period, String dueDate, String payment, String principal,
                                  String interest, String endingBalance) {
    }

    /**
     * Payment schedule for one debt (GET /debts/{id}/schedule). The projection mirrors the debt
     * card's projection; BLOCKED/COMPLETED carry an empty row list plus the machine-readable reason.
     *
     * <p>Date semantics: {@code payoffDate} follows the amortization oracle rule
     * {@code asOf + numberOfPayments months}, while each row's {@code dueDate} follows the
     * contractual {@code dueDay} calendar (clamped to month length). With a {@code dueDay} set, the
     * final row's {@code dueDate} and {@code payoffDate} can differ by days — both are correct for
     * their own purpose: "when in the calendar do payments land" vs "the projection period count".
     */
    public record DebtScheduleView(String status, String payoffDate, Integer numberOfPayments,
                                   String totalInterest, String finalPayment,
                                   String reasonCode, String explanation, String currency,
                                   List<ScheduleRowView> rows) {
    }

    public record DebtSummaryView(String totalOutstandingDebt, String totalMinimumMonthlyPayment,
                                  String totalPlannedMonthlyPayment, String totalMonthlyAccruedInterest,
                                  String currency,
                                  DtiView debtToIncome,
                                  PortfolioProjectionView portfolioProjection,
                                  int blockedDebtCount, String asOf) {
    }
}
