package com.financialgps.application.gps.port.out;

import com.financialgps.domain.gps.GpsProfileSnapshot;
import com.financialgps.domain.model.OwnerId;

import java.time.LocalDate;

/**
 * Port for reading financial profile data for GPS calculation.
 */
public interface GpsProfileReader {

    GpsProfileSnapshot getProfile(OwnerId owner, LocalDate asOfDate);
}