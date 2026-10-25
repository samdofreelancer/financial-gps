package com.financialgps.infrastructure.time;

import com.financialgps.application.gps.port.out.GpsBusinessDate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * GPS adapter for {@link GpsBusinessDate}: delegates to {@link SystemBusinessDate},
 * which stays the single production place allowed to read the wall clock.
 */
@Component
public class GpsBusinessDateAdapter implements GpsBusinessDate {

    private final SystemBusinessDate systemBusinessDate;

    public GpsBusinessDateAdapter(SystemBusinessDate systemBusinessDate) {
        this.systemBusinessDate = systemBusinessDate;
    }

    @Override
    public LocalDate today() {
        return systemBusinessDate.today();
    }
}
