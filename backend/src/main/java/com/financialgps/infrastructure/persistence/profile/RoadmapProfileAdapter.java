package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.gps.port.out.GpsDebtReader;
import com.financialgps.application.gps.port.out.GpsProfileReader;
import com.financialgps.application.profile.port.out.ProfileStore;
import com.financialgps.application.profile.usecase.ProfileAssembler;
import com.financialgps.application.roadmap.port.out.RoadmapProfileReader;
import com.financialgps.domain.gps.CurrentPositionCalculator;
import com.financialgps.domain.gps.GpsCurrentPosition;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.RoadmapInput;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Roadmap adapter for the route currency and Available Capacity.
 *
 * <p>Both quantities are reused, never re-derived: the currency is the 001
 * {@link ProfileAssembler#currencyOf} default (VND when no profile is stored), and Available
 * Capacity is the canonical 004 {@link CurrentPositionCalculator} over the same profile snapshot
 * and debt summary the GPS reads.
 *
 * <p>No profile ⇒ no capacity: the value is {@code null} and the generator reports
 * {@code MISSING_FINANCIAL_PROFILE} instead of inventing a zero.
 */
@Component
public class RoadmapProfileAdapter implements RoadmapProfileReader {

    private final ProfileStore profiles;
    private final GpsProfileReader profileReader;
    private final GpsDebtReader debtReader;

    public RoadmapProfileAdapter(ProfileStore profiles, GpsProfileReader profileReader,
                                 GpsDebtReader debtReader) {
        this.profiles = profiles;
        this.profileReader = profileReader;
        this.debtReader = debtReader;
    }

    @Override
    public RoadmapInput.ProfileSnapshot profile(OwnerId owner, LocalDate asOfDate) {
        String currency = ProfileAssembler.currencyOf(profiles.findByOwner(owner).orElse(null));
        GpsCurrentPosition position = CurrentPositionCalculator.calculate(
                profileReader.getProfile(owner, asOfDate),
                debtReader.getDebtSummary(owner, asOfDate),
                asOfDate);
        String availableCapacity = position.availableCapacity() instanceof GpsCurrentPosition.Available available
                ? available.amount().asDecimalString()
                : null;
        return new RoadmapInput.ProfileSnapshot(currency, availableCapacity);
    }
}
