package com.financialgps.infrastructure.configuration;

import com.financialgps.application.roadmap.port.in.GetRoadmap;
import com.financialgps.application.roadmap.port.out.RoadmapBusinessDate;
import com.financialgps.application.roadmap.port.out.RoadmapDebtReader;
import com.financialgps.application.roadmap.port.out.RoadmapGoalReader;
import com.financialgps.application.roadmap.port.out.RoadmapProfileReader;
import com.financialgps.application.roadmap.usecase.GetRoadmapUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Roadmap use case configuration.
 *
 * <p>Adapters are auto-registered via {@code @Component}; this configuration only wires the use
 * case. The roadmap persists nothing, so the input port is demarcated read-only at the boundary —
 * a read can never mutate a stored fact (spec 005 FR-010).
 *
 * <p>It lives in this package on purpose: {@link UseCaseTransactions} is package-private.
 */
@Configuration
public class RoadmapUseCaseConfiguration {

    @Bean
    GetRoadmap getRoadmap(RoadmapProfileReader profileReader,
                          RoadmapDebtReader debtReader,
                          RoadmapGoalReader goalReader,
                          RoadmapBusinessDate businessDate,
                          UseCaseTransactions transactions) {
        return transactions.readOnly(new GetRoadmapUseCase(
                profileReader, debtReader, goalReader, businessDate));
    }
}
