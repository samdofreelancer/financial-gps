package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GPS adapter for reading debt summary.
 * Converts DebtModels.DebtSummaryView to domain DebtSummaryResult.
 */
@Component
public class GpsDebtAdapter implements com.financialgps.application.gps.port.out.GpsDebtReader {

    private final GetDebtSummary debtSummaryUseCase;
    private final DebtBusinessDate debtBusinessDate;

    public GpsDebtAdapter(GetDebtSummary debtSummaryUseCase, DebtBusinessDate debtBusinessDate) {
        this.debtSummaryUseCase = debtSummaryUseCase;
        this.debtBusinessDate = debtBusinessDate;
    }

    @Override
    public DebtSummaryResult getDebtSummary(OwnerId owner, LocalDate asOfDate) {
        DebtModels.DebtSummaryView view = debtSummaryUseCase.summary(owner);
        return convertToDomain(view, asOfDate);
    }

    private DebtSummaryResult convertToDomain(DebtModels.DebtSummaryView view, LocalDate asOfDate) {
        BigDecimal totalOutstanding = new BigDecimal(view.totalOutstandingDebt());
        BigDecimal totalMin = new BigDecimal(view.totalMinimumMonthlyPayment());
        BigDecimal totalPlanned = new BigDecimal(view.totalPlannedMonthlyPayment());
        BigDecimal totalAccrued = view.totalMonthlyAccruedInterest() != null
                ? new BigDecimal(view.totalMonthlyAccruedInterest())
                : null;

        DebtSummaryResult.DtiResult dti = new DebtSummaryResult.DtiResult(
                view.debtToIncome().status(),
                view.debtToIncome().ratio() != null ? new BigDecimal(view.debtToIncome().ratio()) : null,
                view.debtToIncome().reasonCode(),
                view.debtToIncome().explanation()
        );

        List<DebtSummaryResult.BlockedDebt> blockedDebts = view.portfolioProjection().blockedDebts().stream()
                .map(b -> new DebtSummaryResult.BlockedDebt(b.creditor(), b.reasonCode(), b.explanation()))
                .collect(Collectors.toList());

        DebtSummaryResult.PortfolioProjectionResult projection;
        if ("COMPLETED".equals(view.portfolioProjection().status())) {
            projection = new DebtSummaryResult.PortfolioProjectionResult(
                    com.financialgps.domain.debt.ProjectionStatus.COMPLETED,
                    asOfDate, 0, BigDecimal.ZERO.setScale(2), "DEBT_ALREADY_PAID",
                    "No active debts: the portfolio is already debt-free.", List.of());
        } else if ("BLOCKED".equals(view.portfolioProjection().status())) {
            projection = new DebtSummaryResult.PortfolioProjectionResult(
                    com.financialgps.domain.debt.ProjectionStatus.BLOCKED,
                    null, null, null, view.portfolioProjection().reasonCode(),
                    view.portfolioProjection().explanation(), blockedDebts);
        } else {
            // AVAILABLE
            LocalDate projectedDate = view.portfolioProjection().projectedDebtFreeDate() != null
                    ? LocalDate.parse(view.portfolioProjection().projectedDebtFreeDate())
                    : null;
            projection = new DebtSummaryResult.PortfolioProjectionResult(
                    com.financialgps.domain.debt.ProjectionStatus.AVAILABLE,
                    projectedDate,
                    view.portfolioProjection().totalMonthsRemaining(),
                    view.portfolioProjection().totalInterestRemaining() != null
                            ? new BigDecimal(view.portfolioProjection().totalInterestRemaining())
                            : BigDecimal.ZERO.setScale(2),
                    null,
                    view.portfolioProjection().explanation(),
                    blockedDebts);
        }

        return new DebtSummaryResult(
                totalOutstanding, totalMin, totalPlanned, totalAccrued,
                view.currency(), dti, projection, view.blockedDebtCount(), asOfDate);
    }
}