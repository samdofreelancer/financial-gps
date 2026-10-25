package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Explanation: rule evaluation that produced a GPS result field.
 * Every status, ETA, blocker, and next action must be explained by naming
 * the input values, the rule applied, and the threshold that decided it.
 */
public final class GpsExplanation {

    private GpsExplanation() {
    }

    public enum Category {
        POSITION,
        DISTANCE,
        PROGRESS,
        CAPACITY_COMPARISON,
        ETA,
        STATUS,
        BLOCKER,
        NEXT_ACTION,
        PROVENANCE
    }

    public record Explanation(
            Category category,
            String field,              // e.g., "status", "eta.date", "availableCapacity"
            String rule,               // human-readable rule description
            List<InputReference> inputs,
            String threshold,          // the threshold that was compared (e.g., "latenessTolerance=3")
            String outcome,            // e.g., "lateness=2 <= 3 → AT_RISK"
            GpsProvenance.Entry provenance
    ) {
        public Explanation {
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(rule, "rule");
            Objects.requireNonNull(inputs, "inputs");
            Objects.requireNonNull(threshold, "threshold");
            Objects.requireNonNull(outcome, "outcome");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record InputReference(
            String name,
            String value,              // string representation
            String provenanceKind      // "actual" | "assumed" | "calculated"
    ) {
        public InputReference {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(provenanceKind, "provenanceKind");
        }
    }
}