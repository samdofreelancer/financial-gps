package com.financialgps.domain.engine;

/** Provenance label: actual (fact) vs assumed vs calculated. */
public record Provenance(String field, String kind, String detail) {
}
