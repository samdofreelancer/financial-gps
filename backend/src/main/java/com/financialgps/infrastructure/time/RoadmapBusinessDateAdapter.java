package com.financialgps.infrastructure.time;

import com.financialgps.application.roadmap.port.out.RoadmapBusinessDate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Roadmap adapter for {@link RoadmapBusinessDate}: delegates to {@link SystemBusinessDate}, the
 * single production place allowed to read the wall clock.
 */
@Component
public class RoadmapBusinessDateAdapter implements RoadmapBusinessDate {

    private final SystemBusinessDate systemBusinessDate;

    public RoadmapBusinessDateAdapter(SystemBusinessDate systemBusinessDate) {
        this.systemBusinessDate = systemBusinessDate;
    }

    @Override
    public LocalDate today() {
        return systemBusinessDate.today();
    }
}
