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
                           String status, String currency, ProjectionView projection) {
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

    public record DebtSummaryView(String totalOutstandingDebt, String totalMinimumMonthlyPayment,
                                  String totalPlannedMonthlyPayment, String totalMonthlyAccruedInterest,
                                  String currency,
                                  DtiView debtToIncome,
                                  PortfolioProjectionView portfolioProjection,
                                  int blockedDebtCount, String asOf) {
    }
}
