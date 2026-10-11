package com.financialgps.application.gps.port.out;

import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.domain.model.OwnerId;

/**
 * Port for reading financial profile data for GPS calculation.
 */
public interface GpsProfileReader {

    ProfileModels.ProfileView getProfile(OwnerId owner);
}