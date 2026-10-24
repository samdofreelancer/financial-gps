package com.financialgps.domain.roadmap;

import java.util.Objects;

/**
 * One provenance entry for a derived route field (oracle §11).
 *
 * <p>{@code kind} is {@link #ACTUAL} (a stored fact quoted unchanged), {@link #CALCULATED} (a
 * value a domain service computed) or {@link #USER_ASSUMED} (the assumption a projection rests
 * on). {@code detail} is user-readable and never embeds a capacity-shortfall amount, so the 004
 * shortfall can never leak through provenance (RM-015).
 */
public record ProvenanceEntry(String field, String kind, String detail) {

    public static final String ACTUAL = "actual";
    public static final String CALCULATED = "calculated";
    public static final String USER_ASSUMED = "user-assumed";

    public ProvenanceEntry {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(detail, "detail");
    }

    public static ProvenanceEntry actual(String field, String detail) {
        return new ProvenanceEntry(field, ACTUAL, detail);
    }

    public static ProvenanceEntry calculated(String field, String detail) {
        return new ProvenanceEntry(field, CALCULATED, detail);
    }

    public static ProvenanceEntry assumed(String field, String detail) {
        return new ProvenanceEntry(field, USER_ASSUMED, detail);
    }
}
