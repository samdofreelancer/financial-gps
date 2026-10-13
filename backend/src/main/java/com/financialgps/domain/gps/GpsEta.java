package com.financialgps.domain.gps;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Estimated Time of Arrival for the GPS destination.
 * Either CALCULATED with a date and period count, or UNAVAILABLE with a reason.
 */
public final class GpsEta {

    private GpsEta() {
    }

    public enum Availability {
        CALCULATED, UNAVAILABLE
    }

    public enum Reason {
        MISSING_FINANCIAL_PROFILE,
        NO_AVAILABLE_CAPACITY,
        PAYMENT_DOES_NOT_COVER_INTEREST,
        PAYMENT_COVERS_ONLY_INTEREST,
        INTEREST_RATE_MISSING,
        PAYOFF_HORIZON_EXCEEDS_MAXIMUM,
        PORTFOLIO_CONTAINS_BLOCKED_DEBTS
    }

    public record Calculated(
            LocalDate date,
            int periods,
            GpsProvenance.Entry provenance
    ) {
        public Calculated {
            Objects.requireNonNull(date, "date");
            if (periods < 0) {
                throw new IllegalArgumentException("periods must be >= 0");
            }
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record Unavailable(
            Reason reason,
            String explanation,
            GpsProvenance.Entry provenance
    ) {
        public Unavailable {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(explanation, "explanation");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public interface Eta {
        Availability availability();
    }

    public static final class CalculatedEta implements Eta {
        private final Calculated value;

        public CalculatedEta(Calculated value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public Availability availability() {
            return Availability.CALCULATED;
        }

        public Calculated value() {
            return value;
        }

        public LocalDate date() {
            return value.date();
        }

        public int periods() {
            return value.periods();
        }

        public GpsProvenance.Entry provenance() {
            return value.provenance();
        }
    }

    public static final class UnavailableEta implements Eta {
        private final Unavailable value;

        public UnavailableEta(Unavailable value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public Availability availability() {
            return Availability.UNAVAILABLE;
        }

        public Unavailable value() {
            return value;
        }

        public Reason reason() {
            return value.reason();
        }

        public String explanation() {
            return value.explanation();
        }

        public GpsProvenance.Entry provenance() {
            return value.provenance();
        }
    }

    public static CalculatedEta calculated(LocalDate date, int periods, GpsProvenance.Entry provenance) {
        return new CalculatedEta(new Calculated(date, periods, provenance));
    }

    public static UnavailableEta unavailable(Reason reason, String explanation, GpsProvenance.Entry provenance) {
        return new UnavailableEta(new Unavailable(reason, explanation, provenance));
    }
}