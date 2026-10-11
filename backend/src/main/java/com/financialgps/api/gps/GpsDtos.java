package com.financialgps.api.gps;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * GPS REST DTOs.
 */
public final class GpsDtos {

    private GpsDtos() {
    }

    public record GpsRequest(
            @NotNull UUID goalId,
            LocalDate asOf
    ) {
    }
}