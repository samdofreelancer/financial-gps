package com.financialgps.application.profile.port.in;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;

/** Use case: create-or-replace the owner's profile (PUT /profile). */
public interface PutProfile {

    ProfileModels.ProfileView put(OwnerId owner, ProfileModels.PutProfileCommand command);
}
