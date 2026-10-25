package com.financialgps.application.gps.model;

import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.domain.gps.FinancialGpsResult;
import com.financialgps.domain.gps.GpsBlocker;
import com.financialgps.domain.gps.GpsCurrentPosition;
import com.financialgps.domain.gps.GpsDistance;
import com.financialgps.domain.gps.GpsEta;
import com.financialgps.domain.gps.GpsExplanation;
import com.financialgps.domain.gps.GpsNextAction;
import com.financialgps.domain.gps.GpsProvenance;
import com.financialgps.domain.gps.GpsRouteContext;
import com.financialgps.domain.gps.GpsStatus;
import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * GPS REST DTOs.
 */
public final class GpsModels {

    private GpsModels() {
    }

    public record FinancialGpsView(
            String asOf,
            DestinationView destination,
            CurrentPositionView currentPosition,
            DistanceView distance,
            ProgressView progress,
            CapacityComparisonView capacityComparison,
            EtaView eta,
            StatusView status,
            List<BlockerView> blockers,
            List<NextActionView> nextActions,
            List<ExplanationView> explanations,
            List<ProvenanceView> provenance,
            List<String> missingInputs
    ) {
        public FinancialGpsView {
            Objects.requireNonNull(asOf, "asOf");
            Objects.requireNonNull(destination, "destination");
            Objects.requireNonNull(currentPosition, "currentPosition");
            Objects.requireNonNull(distance, "distance");
            Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(capacityComparison, "capacityComparison");
            Objects.requireNonNull(eta, "eta");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(blockers, "blockers");
            Objects.requireNonNull(nextActions, "nextActions");
            Objects.requireNonNull(explanations, "explanations");
            Objects.requireNonNull(provenance, "provenance");
            Objects.requireNonNull(missingInputs, "missingInputs");
        }

        public static FinancialGpsView from(FinancialGpsResult result) {
            return new FinancialGpsView(
                    result.asOf().toString(),
                    DestinationView.from(result.destination()),
                    CurrentPositionView.from(result.currentPosition()),
                    DistanceView.from(result.distance(), result.destination()),
                    ProgressView.from(result.distance(), result.destination()),
                    CapacityComparisonView.from(result.capacityComparison()),
                    EtaView.from(result.eta()),
                    StatusView.from(result.status()),
                    result.blockers().stream().map(BlockerView::from).collect(Collectors.toList()),
                    result.nextActions().stream().map(NextActionView::from).collect(Collectors.toList()),
                    result.explanations().stream().map(ExplanationView::from).collect(Collectors.toList()),
                    result.provenance().stream().map(ProvenanceView::from).collect(Collectors.toList()),
                    result.missingInputs()
            );
        }
    }

    public record DestinationView(
            String type,        // "GOAL" | "DEBT_FREEDOM"
            String goalId,
            String goalName,
            String goalType
    ) {
        public static DestinationView from(FinancialGpsResult.Destination d) {
            return new DestinationView(
                    d.type().name(),
                    d.goalId(),
                    d.goalName(),
                    d.goalType()
            );
        }
    }

    public record CurrentPositionView(
            MoneyView income,
            MoneyView expense,
            MoneyView mandatoryPayment,
            MoneyView netCashFlow,
            MoneyView availableCapacity,
            MoneyView savings,
            MoneyView emergencyFund,
            Integer dependents,
            MoneyView totalOutstandingDebt,
            DtiView dti
    ) {
        public static CurrentPositionView from(GpsCurrentPosition pos) {
            return new CurrentPositionView(
                    MoneyView.from(pos.income()),
                    MoneyView.from(pos.expense()),
                    MoneyView.from(pos.mandatoryPayment()),
                    MoneyView.from(pos.netCashFlow()),
                    MoneyView.from(pos.availableCapacity()),
                    MoneyView.from(pos.savings()),
                    MoneyView.from(pos.emergencyFund()),
                    pos.dependents(),
                    MoneyView.from(pos.totalOutstandingDebt()),
                    DtiView.from(pos.dti())
            );
        }
    }

    public record MoneyView(
            String field,
            String amount,      // decimal string or null if unavailable
            String currency,
            String availability, // "AVAILABLE" | "UNAVAILABLE"
            String provenanceKind, // "actual" | "assumed" | "calculated" | "unavailable"
            String provenanceDetail,
            String assumptionSource // null or "USER_SUPPLIED" | "SYSTEM_DEFAULT"
    ) {
        public static MoneyView from(GpsCurrentPosition.MoneyValue mv) {
            if (mv instanceof GpsCurrentPosition.Available av) {
                return new MoneyView(
                        av.field(),
                        av.amount().asDecimalString(),
                        av.amount().currency(),
                        "AVAILABLE",
                        av.provenance().kind(),
                        av.provenance().detail(),
                        av.provenance().assumptionSource()
                );
            } else {
                GpsCurrentPosition.Unavailable un = (GpsCurrentPosition.Unavailable) mv;
                return new MoneyView(
                        un.field(),
                        null,
                        un.currency(),
                        "UNAVAILABLE",
                        "unavailable",
                        un.provenance().reasonCode() + ": " + un.provenance().explanation(),
                        null
                );
            }
        }
    }

    public record DtiView(
            String status,      // "AVAILABLE" | "UNAVAILABLE"
            String ratio,       // decimal string or null
            String reasonCode,
            String explanation,
            String provenanceKind,
            String provenanceDetail
    ) {
        public static DtiView from(GpsCurrentPosition.DtiValue dti) {
            if (dti == null) {
                return new DtiView("UNAVAILABLE", null, "MISSING", "DTI not available", "unavailable", "DTI not available");
            }
            String provenanceKind = dti.provenance() instanceof GpsProvenance.AvailableEntry ? "calculated" : "unavailable";
            String provenanceDetail = dti.provenance() instanceof GpsProvenance.AvailableEntry ae
                    ? ae.value().detail() : ((GpsProvenance.UnavailableEntry) dti.provenance()).value().explanation();
            return new DtiView(
                    dti.status(),
                    dti.ratio() != null ? dti.ratio().toPlainString() : null,
                    dti.reasonCode(),
                    dti.explanation(),
                    provenanceKind,
                    provenanceDetail
            );
        }
    }

    public record DistanceView(
            String type,        // "AMOUNT_BASED" | "DEBT_FREEDOM"
            String remaining,   // decimal string
            String currency,
            String targetAmount,     // null for debt-freedom
            String currentAmount     // null for debt-freedom
    ) {
        public static DistanceView from(Object distance, FinancialGpsResult.Destination destination) {
            if (distance instanceof GpsDistance.AmountBased ab) {
                return new DistanceView(
                        "AMOUNT_BASED",
                        ab.remaining().asDecimalString(),
                        ab.remaining().currency(),
                        ab.targetAmount().asDecimalString(),
                        ab.currentAmount().asDecimalString()
                );
            } else {
                GpsDistance.DebtFreedom df = (GpsDistance.DebtFreedom) distance;
                return new DistanceView(
                        "DEBT_FREEDOM",
                        df.totalOutstandingDebt().asDecimalString(),
                        df.totalOutstandingDebt().currency(),
                        null,
                        null
                );
            }
        }
    }

    public record ProgressView(
            String progressPercent,   // decimal string scale 4 or null
            String reason             // null or "PROGRESS_NOT_MEASURABLE"
    ) {
        public static ProgressView from(Object distance, FinancialGpsResult.Destination destination) {
            if (distance instanceof GpsDistance.AmountBased ab) {
                String progress = ab.progressPercent() != null
                        ? ab.progressPercent().toPlainString()
                        : "1.0000";
                return new ProgressView(progress, null);
            } else {
                return new ProgressView(null, "PROGRESS_NOT_MEASURABLE");
            }
        }
    }

    public record CapacityComparisonView(
            String requiredMonthly,     // decimal string
            String projectedMonthly,    // decimal string
            String coverage,            // "MEETS_REQUIRED" | "SHORTFALL" | "NOT_APPLICABLE"
            String shortfall,           // decimal string or null
            String currency,
            String explanation
    ) {
        public static CapacityComparisonView from(FinancialGpsResult.CapacityComparison cc) {
            return new CapacityComparisonView(
                    cc.requiredMonthly().asDecimalString(),
                    cc.projectedMonthly().asDecimalString(),
                    cc.coverage(),
                    cc.shortfall() != null ? cc.shortfall().asDecimalString() : null,
                    cc.requiredMonthly().currency(),
                    cc.explanation()
            );
        }
    }

    public record EtaView(
            String availability,        // "CALCULATED" | "UNAVAILABLE"
            String date,                // ISO date or null
            Integer periods,            // period count or null
            String reason,              // reason code or null
            String explanation
    ) {
        public static EtaView from(GpsEta.Eta eta) {
            if (eta instanceof GpsEta.CalculatedEta ce) {
                return new EtaView(
                        "CALCULATED",
                        ce.date().toString(),
                        ce.periods(),
                        null,
                        null
                );
            } else {
                GpsEta.UnavailableEta ue = (GpsEta.UnavailableEta) eta;
                return new EtaView(
                        "UNAVAILABLE",
                        null,
                        null,
                        ue.reason().name(),
                        ue.explanation()
                );
            }
        }
    }

    public record StatusView(
            String status,              // "COMPLETED" | "BLOCKED" | "ON_TRACK" | "AT_RISK" | "OFF_TRACK"
            String explanation,
            ConditionView condition
    ) {
        public static StatusView from(GpsStatus.Result result) {
            return new StatusView(
                    result.status().name(),
                    result.explanation(),
                    ConditionView.from(result.condition())
            );
        }
    }

    public record ConditionView(
            String type,                // "COMPLETED" | "BLOCKED" | "ON_TRACK" | "AT_RISK" | "OFF_TRACK" | "UNDATED_ON_TRACK" | "DEBT_FREEDOM_ON_TRACK" | "DEBT_FREEDOM_AT_RISK" | "DEBT_FREEDOM_OFF_TRACK"
            String reason,              // for COMPLETED/BLOCKED
            String requiredMonthly,     // for ON_TRACK/AT_RISK/OFF_TRACK
            String projectedMonthly,
            Integer monthsRemaining,
            Integer etaPeriods,
            Integer lateness,
            Integer latenessTolerance,
            String projectedDebtFreeDate,
            Integer totalMonthsRemaining,
            String targetDate
    ) {
        public static ConditionView from(GpsStatus.EvaluatedCondition condition) {
            if (condition instanceof GpsStatus.Completed c) {
                return new ConditionView("COMPLETED", c.reason(), null, null, null, null, null, null, null, null, null);
            } else if (condition instanceof GpsStatus.Blocked b) {
                return new ConditionView("BLOCKED", b.reasonCode(), null, null, null, null, null, null, null, null, null);
            } else if (condition instanceof GpsStatus.OnTrack ot) {
                return new ConditionView("ON_TRACK", null,
                        ot.requiredMonthly().asDecimalString(),
                        ot.projectedMonthly().asDecimalString(),
                        ot.monthsRemaining(), ot.etaPeriods(), ot.lateness(), null, null, null, null);
            } else if (condition instanceof GpsStatus.AtRisk ar) {
                return new ConditionView("AT_RISK", null,
                        ar.requiredMonthly().asDecimalString(),
                        ar.projectedMonthly().asDecimalString(),
                        ar.monthsRemaining(), ar.etaPeriods(), ar.lateness(), ar.latenessTolerance(), null, null, null);
            } else if (condition instanceof GpsStatus.OffTrack ot) {
                return new ConditionView("OFF_TRACK", null,
                        ot.requiredMonthly().asDecimalString(),
                        ot.projectedMonthly().asDecimalString(),
                        ot.monthsRemaining(), ot.etaPeriods(), ot.lateness(), ot.latenessTolerance(), null, null, null);
            } else if (condition instanceof GpsStatus.UndatedOnTrack uot) {
                return new ConditionView("UNDATED_ON_TRACK", null,
                        null, uot.projectedMonthly().asDecimalString(),
                        null, uot.etaPeriods(), null, null, null, null, null);
            } else if (condition instanceof GpsStatus.DebtFreedomOnTrack dfot) {
                return new ConditionView("DEBT_FREEDOM_ON_TRACK", null,
                        null, null,
                        null, null, dfot.lateness(), null,
                        dfot.projectedDebtFreeDate().toString(),
                        dfot.totalMonthsRemaining(),
                        dfot.targetDate() != null ? dfot.targetDate().toString() : null);
            } else if (condition instanceof GpsStatus.DebtFreedomAtRisk dfar) {
                return new ConditionView("DEBT_FREEDOM_AT_RISK", null,
                        null, null,
                        null, null, dfar.lateness(), dfar.latenessTolerance(),
                        dfar.projectedDebtFreeDate().toString(),
                        dfar.totalMonthsRemaining(),
                        dfar.targetDate().toString());
            } else if (condition instanceof GpsStatus.DebtFreedomOffTrack dfot) {
                return new ConditionView("DEBT_FREEDOM_OFF_TRACK", null,
                        null, null,
                        null, null, dfot.lateness(), dfot.latenessTolerance(),
                        dfot.projectedDebtFreeDate().toString(),
                        dfot.totalMonthsRemaining(),
                        dfot.targetDate().toString());
            } else {
                return new ConditionView("UNKNOWN", "Unknown condition", null, null, null, null, null, null, null, null, null);
            }
        }
    }

    public record BlockerView(
            String code,
            String explanation,
            List<String> inputs,
            String provenanceKind,
            String provenanceDetail
    ) {
        public static BlockerView from(GpsBlocker.Blocker b) {
            String provenanceKind = b.provenance() instanceof GpsProvenance.AvailableEntry ? "calculated" : "unavailable";
            String provenanceDetail = b.provenance() instanceof GpsProvenance.AvailableEntry ae
                    ? ae.value().detail() : ((GpsProvenance.UnavailableEntry) b.provenance()).value().explanation();
            return new BlockerView(b.code().name(), b.explanation(), b.inputs(), provenanceKind, provenanceDetail);
        }
    }

    public record NextActionView(
            String type,
            String description,
            List<String> linkedBlockerCodes,
            String provenanceKind,
            String provenanceDetail
    ) {
        public static NextActionView from(GpsNextAction.Action a) {
            String provenanceKind = a.provenance() instanceof GpsProvenance.AvailableEntry ? "calculated" : "unavailable";
            String provenanceDetail = a.provenance() instanceof GpsProvenance.AvailableEntry ae
                    ? ae.value().detail() : ((GpsProvenance.UnavailableEntry) a.provenance()).value().explanation();
            return new NextActionView(a.type().name(), a.description(), a.linkedBlockerCodes(), provenanceKind, provenanceDetail);
        }
    }

    public record ExplanationView(
            String category,
            String field,
            String rule,
            List<InputRefView> inputs,
            String threshold,
            String outcome,
            String provenanceKind,
            String provenanceDetail
    ) {
        public static ExplanationView from(GpsExplanation.Explanation e) {
            String provenanceKind = e.provenance() instanceof GpsProvenance.AvailableEntry ? "calculated" : "unavailable";
            String provenanceDetail = e.provenance() instanceof GpsProvenance.AvailableEntry ae
                    ? ae.value().detail() : ((GpsProvenance.UnavailableEntry) e.provenance()).value().explanation();
            return new ExplanationView(
                    e.category().name(),
                    e.field(),
                    e.rule(),
                    e.inputs().stream().map(InputRefView::from).collect(Collectors.toList()),
                    e.threshold(),
                    e.outcome(),
                    provenanceKind,
                    provenanceDetail
            );
        }
    }

    public record InputRefView(
            String name,
            String value,
            String provenanceKind
    ) {
        public static InputRefView from(GpsExplanation.InputReference ir) {
            return new InputRefView(ir.name(), ir.value(), ir.provenanceKind());
        }
    }

    public record ProvenanceView(
            String field,
            String kind,           // "actual" | "assumed" | "calculated" | "unavailable"
            String detail,
            String assumptionSource
    ) {
        public static ProvenanceView from(GpsProvenance.Entry entry) {
            if (entry instanceof GpsProvenance.AvailableEntry ae) {
                return new ProvenanceView(
                        ae.value().field(),
                        ae.value().kind(),
                        ae.value().detail(),
                        ae.value().assumptionSource()
                );
            } else {
                GpsProvenance.UnavailableEntry ue = (GpsProvenance.UnavailableEntry) entry;
                return new ProvenanceView(
                        ue.value().field(),
                        "unavailable",
                        ue.value().reasonCode() + ": " + ue.value().explanation(),
                        null
                );
            }
        }
    }
}