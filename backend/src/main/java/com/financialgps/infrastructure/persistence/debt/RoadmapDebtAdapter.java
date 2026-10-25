package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.roadmap.port.out.RoadmapDebtReader;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.RoadmapInput;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Roadmap adapter over the Feature 002 portfolio projection.
 *
 * <p>Forwards the plan-based summary unchanged: status, the three monthly totals, the projected
 * debt-free date and the blocked-debt reasons. No amortization, payoff date or Debt Extra is
 * recomputed here (spec 005 §7.6, RM-017).
 */
@Component
public class RoadmapDebtAdapter implements RoadmapDebtReader {

    private final GetDebtSummary debts;

    public RoadmapDebtAdapter(GetDebtSummary debts) {
        this.debts = debts;
    }

    @Override
    public RoadmapInput.DebtPortfolioFacts debtPortfolio(OwnerId owner, LocalDate asOfDate) {
        DebtModels.DebtSummaryView view = debts.summary(owner, asOfDate);
        DebtModels.PortfolioProjectionView projection = view.portfolioProjection();
        List<RoadmapInput.DebtPortfolioFacts.BlockedDebt> blocked = projection.blockedDebts().stream()
                .map(debt -> new RoadmapInput.DebtPortfolioFacts.BlockedDebt(
                        debt.creditor(),
                        debt.reasonCode(),
                        debt.explanation() == null ? "This debt cannot be projected." : debt.explanation()))
                .toList();
        return new RoadmapInput.DebtPortfolioFacts(
                projection.status(),
                view.totalOutstandingDebt(),
                view.totalMinimumMonthlyPayment(),
                view.totalPlannedMonthlyPayment(),
                projection.projectedDebtFreeDate(),
                projection.totalMonthsRemaining(),
                projection.reasonCode(),
                projection.explanation(),
                blocked);
    }
}
