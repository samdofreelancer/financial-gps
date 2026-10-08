package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.application.goal.port.out.GoalPositionReader;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.GetProfile;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * Position adapter: reads Available Capacity from the canonical Financial Position view.
 * Delegates to the profile read (which owns the engine call) so 003 never duplicates cash-flow
 * logic and never touches the engine directly.
 */
@Component
class GoalPositionReaderAdapter implements GoalPositionReader {

    private final GetProfile getProfile;

    GoalPositionReaderAdapter(GetProfile getProfile) {
        this.getProfile = getProfile;
    }

    @Override
    public PositionSnapshot snapshot(OwnerId owner) {
        ProfileModels.ProfileView position = getProfile.get(owner);
        String amount = position.availableCapacity() == null ? "0.00"
                : position.availableCapacity().amount();
        String currency = position.currency() == null ? "VND" : position.currency();
        LocalDate asOf = LocalDate.parse(position.asOf());
        return new PositionSnapshot(amount, currency, asOf);
    }
}
