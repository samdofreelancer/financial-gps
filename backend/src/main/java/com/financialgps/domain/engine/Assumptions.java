package com.financialgps.domain.engine;

import java.util.List;

/** Labelled assumption set. Empty for 001 (no user assumptions yet); kept so the signature is canonical. */
public record Assumptions(List<FinancialAssumption> values) {

    public static Assumptions none() {
        return new Assumptions(List.of());
    }

    public Assumptions {
        values = List.copyOf(values);
    }
}
