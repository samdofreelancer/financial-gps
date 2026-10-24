package com.financialgps.domain.roadmap;

import com.financialgps.domain.gps.GpsEtaCalculator;
import com.financialgps.domain.model.Money;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A stage's projected completion date together with the two labels that keep it honest
 * (spec 005 §7.6, decisions D-4 and D-10).
 *
 * <p>This is a <b>forwarded</b> value, never a 005 money formula:
 *
 * <ul>
 *   <li>debt stage — the 002 plan-based portfolio projection, quoted unchanged. It assumes each
 *       stored {@code plannedPayment} (minimum plus the <em>requested</em> Debt Extra) is paid in
 *       full; it is not a route-funded date and no {@code FundedExtra} / effective payment is
 *       derived (RM-017);</li>
 *   <li>goal stage — the 004 single-destination hypothesis, i.e.
 *       {@link GpsEtaCalculator#ceilPeriods} over the whole Available Capacity. It ignores
 *       competition between stages.</li>
 * </ul>
 *
 * <p>{@code ROUTE_PROJECTION} / {@code ROUTE_ALLOCATION} are reserved for Feature 008 and are never
 * produced here: when 008 ships, 005 forwards its date and labels unchanged (RM-018).
 *
 * <p>A {@code null} date always carries a {@code reasonCode} — never a fabricated date, never a
 * silent absence.
 */
public record RoadmapEtaHypothesis(
        LocalDate date,
        EtaBasis basis,
        EtaAssumption assumption,
        String reasonCode) {

    public static final String REASON_MISSING_PROFILE = "MISSING_FINANCIAL_PROFILE";
    public static final String REASON_NO_CAPACITY = "NO_AVAILABLE_CAPACITY";
    public static final String REASON_HORIZON_EXCEEDS_MAXIMUM = "PAYOFF_HORIZON_EXCEEDS_MAXIMUM";
    public static final String REASON_BLOCKED_PORTFOLIO = "PORTFOLIO_CONTAINS_BLOCKED_DEBTS";

    public RoadmapEtaHypothesis {
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(assumption, "assumption");
        if (date == null && reasonCode == null) {
            throw new IllegalArgumentException("an unavailable date must carry a reason code");
        }
        if (date != null && reasonCode != null) {
            throw new IllegalArgumentException("an available date must not carry a reason code");
        }
    }

    public boolean isAvailable() {
        return date != null;
    }

    /**
     * The debt-stage date: the 002 plan-based projection, forwarded unchanged.
     *
     * <p>When the portfolio projection is blocked it supplies no date; its own reason code is
     * forwarded verbatim so 005 never invents a different explanation.
     */
    public static RoadmapEtaHypothesis forDebt(RoadmapInput.DebtPortfolioFacts facts) {
        Objects.requireNonNull(facts, "facts");
        String reason = null;
        LocalDate date = null;
        if (facts.projectedDebtFreeDate() != null) {
            date = LocalDate.parse(facts.projectedDebtFreeDate());
        } else {
            reason = facts.reasonCode() != null ? facts.reasonCode() : REASON_BLOCKED_PORTFOLIO;
        }
        return new RoadmapEtaHypothesis(date, EtaBasis.PORTFOLIO_PROJECTION,
                EtaAssumption.PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA, reason);
    }

    /**
     * The goal-stage date: {@code asOf.plusMonths(ceil(remaining / availableCapacity))}.
     *
     * <p>The capacity guard ({@code > 0}) runs <em>before</em> {@code ceilPeriods}, which divides.
     * The calculator's {@link Integer#MAX_VALUE} saturation — and a {@code plusMonths} overflow —
     * are reported as {@code PAYOFF_HORIZON_EXCEEDS_MAXIMUM} instead of a fabricated date.
     *
     * @param remaining        003 remaining amount (zero when the goal is already complete)
     * @param availableCapacity parsed Available Capacity, or {@code null} when unmeasurable
     * @param profileMissing   {@code true} when no Financial Profile exists — capacity is never
     *                         invented for a missing profile
     */
    public static RoadmapEtaHypothesis forGoal(LocalDate asOf, Money remaining,
                                               Money availableCapacity, boolean profileMissing) {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(remaining, "remaining");
        if (remaining.amount().signum() == 0) {
            return goalHypothesis(asOf, null);
        }
        if (profileMissing || availableCapacity == null) {
            return goalHypothesis(null, REASON_MISSING_PROFILE);
        }
        if (availableCapacity.amount().signum() <= 0) {
            return goalHypothesis(null, REASON_NO_CAPACITY);
        }
        int periods = GpsEtaCalculator.ceilPeriods(remaining.amount(), availableCapacity.amount());
        if (periods == Integer.MAX_VALUE) {
            return goalHypothesis(null, REASON_HORIZON_EXCEEDS_MAXIMUM);
        }
        try {
            return goalHypothesis(asOf.plusMonths(periods), null);
        } catch (DateTimeException overflow) {
            return goalHypothesis(null, REASON_HORIZON_EXCEEDS_MAXIMUM);
        }
    }

    private static RoadmapEtaHypothesis goalHypothesis(LocalDate date, String reasonCode) {
        return new RoadmapEtaHypothesis(date, EtaBasis.SINGLE_DESTINATION_HYPOTHESIS,
                EtaAssumption.WHOLE_AVAILABLE_CAPACITY, reasonCode);
    }
}
