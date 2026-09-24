package com.financialgps.domain.engine;

import com.financialgps.domain.finance.CashFlowResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Deterministic output: current position + provenance + explanations. */
public record FinancialResult(
        LocalDate asOfDate,
        CashFlowResult position,
        List<Provenance> provenance,
        List<String> explanations) {

    public FinancialResult {
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(position, "position");
        provenance = List.copyOf(Objects.requireNonNull(provenance, "provenance"));
        explanations = List.copyOf(Objects.requireNonNull(explanations, "explanations"));
    }
}
