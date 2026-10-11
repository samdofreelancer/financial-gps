package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Distance to destination: remaining amount for amount-based goals,
 * total outstanding debt for debt-freedom goals.
 */
public final class GpsDistance {

    private GpsDistance() {
    }

    /** Remaining amount for amount-based destination. */
    public record AmountBased(
            Money remaining,
            Money targetAmount,
            Money currentAmount,
            GpsProvenance.Entry provenance,
            BigDecimal progressPercent  // scale 4, floored at display time; null for debt-freedom
    ) {
        public AmountBased {
            Objects.requireNonNull(remaining, "remaining");
            Objects.requireNonNull(targetAmount, "targetAmount");
            Objects.requireNonNull(currentAmount, "currentAmount");
            Objects.requireNonNull(provenance, "provenance");
            // progressPercent can be null (debt-freedom)
        }

        public boolean isCompleted() {
            return remaining.amount().signum() == 0;
        }
    }

    /** Total outstanding debt for debt-freedom destination. */
    public record DebtFreedom(
            Money totalOutstandingDebt,
            GpsProvenance.Entry provenance
    ) {
        public DebtFreedom {
            Objects.requireNonNull(totalOutstandingDebt, "totalOutstandingDebt");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public interface Distance {
    }
}