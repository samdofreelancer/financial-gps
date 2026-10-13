package com.financialgps.application.gps.port.out;

import java.time.LocalDate;

/**
 * Business date provider for GPS calculation.
 */
public interface GpsBusinessDate {

    LocalDate today();
}