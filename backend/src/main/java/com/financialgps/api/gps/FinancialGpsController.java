package com.financialgps.api.gps;

import com.financialgps.application.gps.model.GpsModels;
import com.financialgps.application.gps.port.in.CalculateFinancialGps;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.platform.security.CurrentOwnerProvider;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter for Financial GPS.
 * GET /api/v1/gps?goalId=...&asOf=...
 */
@RestController
@RequestMapping("/api/v1/gps")
public class FinancialGpsController {

    private final CalculateFinancialGps calculateFinancialGps;
    private final CurrentOwnerProvider owners;

    public FinancialGpsController(CalculateFinancialGps calculateFinancialGps,
                                  CurrentOwnerProvider owners) {
        this.calculateFinancialGps = calculateFinancialGps;
        this.owners = owners;
    }

    @GetMapping
    public GpsModels.FinancialGpsView getGps(
            @RequestParam @Valid UUID goalId,
            @RequestParam(required = false) LocalDate asOf) {
        OwnerId owner = owners.requireCurrentOwner();
        if (asOf != null) {
            return calculateFinancialGps.calculate(owner, goalId, asOf);
        }
        return calculateFinancialGps.calculate(owner, goalId);
    }
}