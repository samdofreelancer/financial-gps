package com.financialgps.infrastructure.time;

import com.financialgps.application.profile.port.out.BusinessDate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Production adapter for {@link BusinessDate}: the system clock, evaluated in the JVM's configured
 * zone (the application boots with {@code user.timezone=UTC}).
 *
 * <p>This is the ONLY production place allowed to read a wall clock for financial use cases — the
 * API adapters must not, and the pure engine keeps receiving an explicit {@code asOf} (acceptance
 * criterion: {@code LocalDate.now()} is absent from API/application financial use cases).
 */
@Component
class SystemBusinessDate implements BusinessDate {

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
