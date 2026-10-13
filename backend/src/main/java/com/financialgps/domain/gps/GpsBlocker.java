package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;

import java.util.List;
import java.util.Objects;

/**
 * Blocker: a verifiable condition that prevents or delays route progress.
 */
public final class GpsBlocker {

    private GpsBlocker() {
    }

    public enum Code {
        NO_AVAILABLE_CAPACITY,
        MISSING_FINANCIAL_PROFILE,
        PAYMENT_DOES_NOT_COVER_INTEREST,
        PAYMENT_COVERS_ONLY_INTEREST,
        INTEREST_RATE_MISSING,
        PAYOFF_HORIZON_EXCEEDS_MAXIMUM,
        PORTFOLIO_CONTAINS_BLOCKED_DEBTS
    }

    public record Blocker(
            Code code,
            String explanation,
            List<String> inputs,      // input names that caused this blocker
            GpsProvenance.Entry provenance
    ) {
        public Blocker {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(explanation, "explanation");
            Objects.requireNonNull(inputs, "inputs");
            Objects.requireNonNull(provenance, "provenance");
        }
    }
}