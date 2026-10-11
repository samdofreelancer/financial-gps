package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * GPS Status evaluation with explanations.
 * Status precedence: COMPLETED -> BLOCKED -> ON_TRACK -> AT_RISK -> OFF_TRACK
 */
public final class GpsStatus {

    private GpsStatus() {
    }

    public enum Status {
        COMPLETED,
        BLOCKED,
        ON_TRACK,
        AT_RISK,
        OFF_TRACK
    }

    /** Policy for status evaluation (configurable lateness tolerance). */
    public record Policy(
            int latenessTolerance  // default 3 contribution periods
    ) {
        public Policy {
            if (latenessTolerance < 0) {
                throw new IllegalArgumentException("latenessTolerance must be >= 0");
            }
        }

        public static Policy defaults() {
            return new Policy(3);
        }
    }

    /** Result of status evaluation with full explanation. */
    public record Result(
            Status status,
            String explanation,
            EvaluatedCondition condition
    ) {
        public Result {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(explanation, "explanation");
            Objects.requireNonNull(condition, "condition");
        }
    }

    /** The evaluated condition that determined the status. */
    public interface EvaluatedCondition {
    }

    /** COMPLETED: destination arrived. */
    public record Completed(
            String reason,  // "AMOUNT_REACHED" or "DEBT_FREE"
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public Completed {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** BLOCKED: no finite route exists. */
    public record Blocked(
            String reasonCode,  // e.g., "NO_AVAILABLE_CAPACITY", "PORTFOLIO_CONTAINS_BLOCKED_DEBTS"
            String explanation,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public Blocked {
            Objects.requireNonNull(reasonCode, "reasonCode");
            Objects.requireNonNull(explanation, "explanation");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** ON_TRACK: finite route, no lateness (lateness = 0). */
    public record OnTrack(
            Money requiredMonthly,
            Money projectedMonthly,
            int monthsRemaining,
            int etaPeriods,
            int lateness,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public OnTrack {
            Objects.requireNonNull(requiredMonthly, "requiredMonthly");
            Objects.requireNonNull(projectedMonthly, "projectedMonthly");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** AT_RISK: 1 <= lateness <= latenessTolerance. */
    public record AtRisk(
            Money requiredMonthly,
            Money projectedMonthly,
            int monthsRemaining,
            int etaPeriods,
            int lateness,
            int latenessTolerance,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public AtRisk {
            Objects.requireNonNull(requiredMonthly, "requiredMonthly");
            Objects.requireNonNull(projectedMonthly, "projectedMonthly");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** OFF_TRACK: lateness > latenessTolerance. */
    public record OffTrack(
            Money requiredMonthly,
            Money projectedMonthly,
            int monthsRemaining,
            int etaPeriods,
            int lateness,
            int latenessTolerance,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public OffTrack {
            Objects.requireNonNull(requiredMonthly, "requiredMonthly");
            Objects.requireNonNull(projectedMonthly, "projectedMonthly");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Undated goal with finite route: ON_TRACK (no lateness possible). */
    public record UndatedOnTrack(
            int etaPeriods,
            Money projectedMonthly,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public UndatedOnTrack {
            Objects.requireNonNull(projectedMonthly, "projectedMonthly");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Debt-freedom with finite portfolio payoff: ON_TRACK. */
    public record DebtFreedomOnTrack(
            LocalDate projectedDebtFreeDate,
            int totalMonthsRemaining,
            LocalDate targetDate,
            int lateness,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public DebtFreedomOnTrack {
            Objects.requireNonNull(projectedDebtFreeDate, "projectedDebtFreeDate");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Debt-freedom AT_RISK (dated). */
    public record DebtFreedomAtRisk(
            LocalDate projectedDebtFreeDate,
            int totalMonthsRemaining,
            LocalDate targetDate,
            int lateness,
            int latenessTolerance,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public DebtFreedomAtRisk {
            Objects.requireNonNull(projectedDebtFreeDate, "projectedDebtFreeDate");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    /** Debt-freedom OFF_TRACK (dated). */
    public record DebtFreedomOffTrack(
            LocalDate projectedDebtFreeDate,
            int totalMonthsRemaining,
            LocalDate targetDate,
            int lateness,
            int latenessTolerance,
            GpsProvenance.Entry provenance
    ) implements EvaluatedCondition {
        public DebtFreedomOffTrack {
            Objects.requireNonNull(projectedDebtFreeDate, "projectedDebtFreeDate");
            Objects.requireNonNull(provenance, "provenance");
        }
    }
}