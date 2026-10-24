package com.financialgps.application.roadmap.port.out;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.RoadmapInput;

import java.time.LocalDate;

/**
 * Port for reading the 002 debt portfolio facts the roadmap quotes.
 *
 * <p>The values are the plan-based portfolio projection as of {@code asOfDate} — 005 forwards them
 * and never recomputes an amortization or a payoff date.
 */
public interface RoadmapDebtReader {

    RoadmapInput.DebtPortfolioFacts debtPortfolio(OwnerId owner, LocalDate asOfDate);
}
