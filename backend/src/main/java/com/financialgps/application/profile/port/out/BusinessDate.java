package com.financialgps.application.profile.port.out;

import java.time.LocalDate;

/**
 * Technical-time port: the single source of the domain-relevant "today" used by the profile use
 * cases (the {@code asOf} evaluation date and a new line's {@code effectiveFrom}).
 *
 * <p>The API adapter MUST NOT supply a date and the pure financial engine keeps receiving an
 * explicit {@code asOf} — production resolves this port from the system clock, tests from a fixed
 * value (plan Phase 3 step 2; acceptance criterion "business date is explicit and controllable").
 */
public interface BusinessDate {

    LocalDate today();
}
