package com.financialgps.application.profile.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;

/** Use case: read the owner's Financial Profile position (GET /profile). */
public interface GetProfile {

    ProfileModels.ProfileView get(OwnerId owner);
}
