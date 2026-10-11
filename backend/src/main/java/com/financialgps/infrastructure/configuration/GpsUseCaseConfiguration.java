package com.financialgps.infrastructure.configuration;

import com.financialgps.application.gps.port.in.CalculateFinancialGps;
import com.financialgps.application.gps.port.out.GpsBusinessDate;
import com.financialgps.application.gps.port.out.GpsDebtReader;
import com.financialgps.application.gps.port.out.GpsGoalReader;
import com.financialgps.application.gps.port.out.GpsProfileReader;
import com.financialgps.application.gps.usecase.CalculateFinancialGpsUseCase;
import com.financialgps.infrastructure.time.SystemBusinessDate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GPS use case configuration.
 * Adapters are auto-registered via @Component; this config only wires the use case.
 */
@Configuration
public class GpsUseCaseConfiguration {

    @Bean
    CalculateFinancialGps calculateFinancialGps(GpsProfileReader profileReader,
                                                GpsGoalReader goalReader,
                                                GpsDebtReader debtReader,
                                                GpsBusinessDate businessDate) {
        return new CalculateFinancialGpsUseCase(profileReader, goalReader, debtReader, businessDate);
    }

    @Bean
    GpsBusinessDate gpsBusinessDate(SystemBusinessDate systemBusinessDate) {
        return systemBusinessDate::today;
    }
}