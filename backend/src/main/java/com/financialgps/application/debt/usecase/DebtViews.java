package com.financialgps.application.debt.usecase;

import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtCalculationPolicy;
import com.financialgps.domain.debt.DebtPayoffCalculator;
import com.financialgps.domain.debt.DebtScheduleEntry;
import com.financialgps.domain.debt.DebtSummaryCalculator;
import com.financialgps.domain.debt.DebtSummaryResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Domain projection -> application view assembly (no formula here). */
final class DebtViews {

    private DebtViews() {
    }

    static DebtModels.DebtView view(Debt debt, LocalDate asOf) {
        var p = DebtPayoffCalculator.project(debt, asOf, DebtCalculationPolicy.defaults());
        return new DebtModels.DebtView(debt.id() == null ? null : debt.id().value().toString(),
                debt.creditor(), debt.debtType().name(),
                debt.originalPrincipal() == null ? null : debt.originalPrincipal().asDecimalString(),
                debt.outstandingBalance().asDecimalString(),
                debt.annualInterestRate() == null ? null : debt.annualInterestRate().asDecimalString(),
                debt.minimumPayment().asDecimalString(), debt.plannedPayment().asDecimalString(),
                debt.dueDay(), debt.status().name(), debt.outstandingBalance().currency(),
                new DebtModels.ProjectionView(p.status().name(),
                        p.projectedPayoffDate() == null ? null : p.projectedPayoffDate().toString(),
                        p.numberOfPayments(),
                        p.totalInterest() == null ? null : p.totalInterest().toPlainString(),
                        p.finalPayment() == null ? null : p.finalPayment().toPlainString(),
                        p.monthlyInterest() == null ? null : p.monthlyInterest().toPlainString(),
                        p.reasonCode(), p.explanation()),
                debt.paymentMarkedOn() != null
                        && YearMonth.from(debt.paymentMarkedOn()).equals(YearMonth.from(asOf)));
    }

    /** Full payment calendar of one debt: the projection plus every period's split (no formula here). */
    static DebtModels.DebtScheduleView scheduleView(Debt debt, LocalDate asOf) {
        var s = DebtPayoffCalculator.schedule(debt, asOf, DebtCalculationPolicy.defaults());
        var p = s.projection();
        List<DebtModels.ScheduleRowView> rows = new ArrayList<>();
        for (DebtScheduleEntry row : s.rows()) {
            rows.add(new DebtModels.ScheduleRowView(row.period(), row.dueDate().toString(),
                    row.payment().toPlainString(), row.principal().toPlainString(),
                    row.interest().toPlainString(), row.endingBalance().toPlainString()));
        }
        return new DebtModels.DebtScheduleView(p.status().name(),
                p.projectedPayoffDate() == null ? null : p.projectedPayoffDate().toString(),
                p.numberOfPayments(),
                p.totalInterest() == null ? null : p.totalInterest().toPlainString(),
                p.finalPayment() == null ? null : p.finalPayment().toPlainString(),
                p.reasonCode(), p.explanation(), debt.outstandingBalance().currency(), List.copyOf(rows));
    }

    static DebtModels.DebtSummaryView summaryView(List<Debt> active, BigDecimal income, LocalDate asOf) {
        String currency = active.isEmpty() ? Debt.DEFAULT_CURRENCY : active.get(0).outstandingBalance().currency();
        DebtSummaryResult r = DebtSummaryCalculator.summarize(active, income, currency,
                asOf, DebtCalculationPolicy.defaults());
        DebtModels.DtiView dti = new DebtModels.DtiView(r.debtToIncome().status(),
                r.debtToIncome().ratio() == null ? null : r.debtToIncome().ratio().toPlainString(),
                r.debtToIncome().reasonCode(), r.debtToIncome().explanation());
        List<DebtModels.BlockedDebtView> blocked = new ArrayList<>();
        for (DebtSummaryResult.BlockedDebt b : r.portfolioProjection().blockedDebts()) {
            blocked.add(new DebtModels.BlockedDebtView(b.creditor(), b.reasonCode(), b.explanation()));
        }
        var projection = new DebtModels.PortfolioProjectionView(
                r.portfolioProjection().status().name(),
                r.portfolioProjection().projectedDebtFreeDate() == null ? null
                        : r.portfolioProjection().projectedDebtFreeDate().toString(),
                r.portfolioProjection().totalMonthsRemaining(),
                r.portfolioProjection().totalInterestRemaining() == null ? null
                        : r.portfolioProjection().totalInterestRemaining().toPlainString(),
                r.portfolioProjection().reasonCode(), r.portfolioProjection().explanation(),
                List.copyOf(blocked));
        return new DebtModels.DebtSummaryView(r.totalOutstandingDebt().toPlainString(),
                r.totalMinimumMonthlyPayment().toPlainString(),
                r.totalPlannedMonthlyPayment().toPlainString(),
                r.totalMonthlyAccruedInterest() == null ? null
                        : r.totalMonthlyAccruedInterest().toPlainString(),
                r.currency(), dti, projection,
                r.blockedDebtCount(), r.asOf().toString());
    }
}
