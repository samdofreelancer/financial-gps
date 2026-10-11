package com.financialgps.application.gps.port.out;

import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.model.OwnerId;

import java.time.LocalDate;

/**
 * Port for reading debt summary for GPS calculation.
 */
public interface GpsDebtReader {

    DebtSummaryResult getDebtSummary(OwnerId owner, LocalDate asOfDate);
}