package com.financialgps.domain.gps;

import com.financialgps.domain.engine.Provenance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Provenance for GPS values: actual (fact), assumed, or calculated.
 * Extended with availability state (AVAILABLE vs UNAVAILABLE) and reason.
 */
public final class GpsProvenance {

    private GpsProvenance() {
    }

    /** A value whose input is present and was used. */
    public record Available(
            String field,
            String kind,              // "actual" | "assumed" | "calculated"
            String detail,            // source or calculation rule
            String assumptionSource   // null or "USER_SUPPLIED" | "SYSTEM_DEFAULT"
    ) {
        public Available {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(detail, "detail");
            if (!kind.equals("actual") && !kind.equals("assumed") && !kind.equals("calculated")) {
                throw new IllegalArgumentException("kind must be actual|assumed|calculated");
            }
        }

        public static Available actual(String field, String detail) {
            return new Available(field, "actual", detail, null);
        }

        public static Available assumed(String field, String detail, String source) {
            return new Available(field, "assumed", detail, Objects.requireNonNull(source, "source"));
        }

        public static Available calculated(String field, String detail) {
            return new Available(field, "calculated", detail, null);
        }
    }

    /** A value that could not be computed because a required input is missing. */
    public record Unavailable(
            String field,
            String reasonCode,        // e.g., "PROFILE_MISSING", "INTEREST_RATE_MISSING"
            String explanation
    ) {
        public Unavailable {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(reasonCode, "reasonCode");
            Objects.requireNonNull(explanation, "explanation");
        }
    }

    public interface Entry {
    }

    public static final class AvailableEntry implements Entry {
        private final Available value;

        public AvailableEntry(Available value) {
            this.value = Objects.requireNonNull(value);
        }

        public Available value() {
            return value;
        }
    }

    public static final class UnavailableEntry implements Entry {
        private final Unavailable value;

        public UnavailableEntry(Unavailable value) {
            this.value = Objects.requireNonNull(value);
        }

        public Unavailable value() {
            return value;
        }
    }

    public static List<Provenance> toEngineProvenance(List<Entry> entries) {
        return entries.stream()
                .map(e -> {
                    if (e instanceof AvailableEntry ae) {
                        Available a = ae.value();
                        return new Provenance(a.field(), a.kind(), a.detail());
                    } else {
                        Unavailable u = ((UnavailableEntry) e).value();
                        return new Provenance(u.field(), "unavailable", u.reasonCode() + ": " + u.explanation());
                    }
                })
                .toList();
    }
}