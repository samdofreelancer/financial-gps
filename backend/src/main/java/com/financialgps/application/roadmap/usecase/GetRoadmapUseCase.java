package com.financialgps.application.roadmap.usecase;

import com.financialgps.application.roadmap.model.RoadmapModels;
import com.financialgps.application.roadmap.port.in.GetRoadmap;
import com.financialgps.application.roadmap.port.out.RoadmapBusinessDate;
import com.financialgps.application.roadmap.port.out.RoadmapDebtReader;
import com.financialgps.application.roadmap.port.out.RoadmapGoalReader;
import com.financialgps.application.roadmap.port.out.RoadmapProfileReader;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.Roadmap;
import com.financialgps.domain.roadmap.RoadmapGenerator;
import com.financialgps.domain.roadmap.RoadmapInput;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Application service for the Financial Roadmap: load the owner's facts, assemble the framework-free
 * {@link RoadmapInput}, call the pure {@link RoadmapGenerator} and map the result to the transport
 * view.
 *
 * <p>Assembly only. No financial formula lives here and no clock is read: the route arithmetic is in
 * {@code domain.roadmap} (reusing the 002/003/004 domain services), and the business date comes from
 * the {@link RoadmapBusinessDate} port.
 */
public final class GetRoadmapUseCase implements GetRoadmap {

    private final RoadmapProfileReader profileReader;
    private final RoadmapDebtReader debtReader;
    private final RoadmapGoalReader goalReader;
    private final RoadmapBusinessDate businessDate;

    public GetRoadmapUseCase(RoadmapProfileReader profileReader,
                             RoadmapDebtReader debtReader,
                             RoadmapGoalReader goalReader,
                             RoadmapBusinessDate businessDate) {
        this.profileReader = Objects.requireNonNull(profileReader, "profileReader");
        this.debtReader = Objects.requireNonNull(debtReader, "debtReader");
        this.goalReader = Objects.requireNonNull(goalReader, "goalReader");
        this.businessDate = Objects.requireNonNull(businessDate, "businessDate");
    }

    @Override
    public RoadmapModels.RoadmapView roadmap(OwnerId owner, LocalDate asOfDate) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(asOfDate, "asOfDate");

        RoadmapInput.ProfileSnapshot profile = profileReader.profile(owner, asOfDate);
        RoadmapInput.DebtPortfolioFacts debts = debtReader.debtPortfolio(owner, asOfDate);
        RoadmapInput input = new RoadmapInput(asOfDate, profile, debts, goalReader.goals(owner));

        Roadmap roadmap = RoadmapGenerator.generate(input);
        return RoadmapModels.RoadmapView.from(roadmap);
    }

    @Override
    public RoadmapModels.RoadmapView roadmap(OwnerId owner) {
        return roadmap(owner, businessDate.today());
    }
}
