package com.financialgps.application.profile.port.in;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;

import java.time.LocalDate;

/** Use case: read the owner's Financial Profile position (GET /profile). */
public interface GetProfile {

    ProfileModels.ProfileView get(OwnerId owner);

    /**
     * Deterministic variant evaluated as of an explicit date. The default delegates to the
     * business-date variant so existing callers keep their behaviour.
     */
    default ProfileModels.ProfileView get(OwnerId owner, LocalDate asOfDate) {
        return get(owner);
    }
}
