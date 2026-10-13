package com.financialgps.application.gps.port.in;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.gps.model.GpsModels;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Input port for calculating Financial GPS.
 */
public interface CalculateFinancialGps {

    GpsModels.FinancialGpsView calculate(OwnerId owner, UUID goalId, LocalDate asOfDate);

    GpsModels.FinancialGpsView calculate(OwnerId owner, UUID goalId);
}