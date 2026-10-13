package com.financialgps.domain.gps;

import java.util.List;
import java.util.Objects;

/**
 * Next action: measurable, non-prescriptive action linked to a blocker or rule.
 * Never issues a command ("you must...") or promises an outcome.
 */
public final class GpsNextAction {

    private GpsNextAction() {
    }

    public enum Type {
        INCREASE_NET_CASH_FLOW,
        RAISE_DEBT_PAYMENT,
        COMPLETE_FINANCIAL_PROFILE,
        SUPPLY_TARGET_DATE,
        SUPPLY_INTEREST_RATE,
        REVIEW_DEBT_TERMS
    }

    public record Action(
            Type type,
            String description,       // e.g., "Increase Net Cash Flow by 2,400,000 VND/month"
            List<String> linkedBlockerCodes,
            GpsProvenance.Entry provenance
    ) {
        public Action {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(linkedBlockerCodes, "linkedBlockerCodes");
            Objects.requireNonNull(provenance, "provenance");
        }
    }
}