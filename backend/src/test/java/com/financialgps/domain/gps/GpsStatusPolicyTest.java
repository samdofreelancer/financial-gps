package com.financialgps.domain.gps;

import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.debt.ProjectionStatus;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 004: GPS status precedence COMPLETED → BLOCKED → ON_TRACK → AT_RISK → OFF_TRACK,
 * debt-freedom completion/reactivation, and the configurable lateness tolerance.
 */
class GpsStatusPolicyTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 11);

    private static Goal amountGoal(String target, String current, LocalDate targetDate) {
        return Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND", target, current, targetDate, 1);
    }

    private static Goal debtFreedomGoal(LocalDate targetDate) {
        return Goal.recorded("VND", "Debt Freedom", "DEBT_FREEDOM", null, null, targetDate, 1);
    }

    private static GpsCurrentPosition position(String capacity) {
        return position(capacity, false);
    }

    private static GpsCurrentPosition position(String capacity, boolean profileMissing) {
        GpsCurrentPosition.Builder b = GpsCurrentPosition.builder();
        Money zero = Money.zero("VND");
        var prov = new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated("t", "t"));
        b.income(new GpsCurrentPosition.Available("income", zero, prov.value()));
        b.expense(new GpsCurrentPosition.Available("expense", zero, prov.value()));
        b.mandatoryPayment(new GpsCurrentPosition.Available("mandatoryPayment", zero, prov.value()));
        b.netCashFlow(new GpsCurrentPosition.Available("netCashFlow", zero, prov.value()));
        if (profileMissing) {
            b.availableCapacity(new GpsCurrentPosition.Unavailable("availableCapacity", "VND",
                    new GpsProvenance.Unavailable("availableCapacity", "PROFILE_MISSING", "no profile")));
        } else {
            b.availableCapacity(new GpsCurrentPosition.Available("availableCapacity",
                    Money.of(capacity, "VND"), prov.value()));
        }
        b.savings(new GpsCurrentPosition.Available("savings", zero, prov.value()));
        b.emergencyFund(new GpsCurrentPosition.Available("emergencyFund", zero, prov.value()));
        b.dependents(0);
        b.totalOutstandingDebt(new GpsCurrentPosition.Available("totalOutstandingDebt", zero, prov.value()));
        b.dti(new GpsCurrentPosition.DtiValue("AVAILABLE", new BigDecimal("0.1000"), null, "dti", prov));
        return b.build();
    }

    private static DebtSummaryResult debts(ProjectionStatus status, LocalDate debtFreeDate,
                                           Integer monthsRemaining, String reasonCode) {
        return new DebtSummaryResult(
                new BigDecimal("120000000.00"), new BigDecimal("20000000.00"),
                new BigDecimal("20000000.00"), null, "VND",
                new DebtSummaryResult.DtiResult("AVAILABLE", new BigDecimal("0.2703"), null, "dti"),
                new DebtSummaryResult.PortfolioProjectionResult(status, debtFreeDate, monthsRemaining,
                        new BigDecimal("1000000.00"), reasonCode,
                        reasonCode == null ? "payoff" : "blocked", List.of()),
                0, AS_OF);
    }

    private static DebtSummaryResult openDebts() {
        return debts(ProjectionStatus.AVAILABLE, AS_OF.plusMonths(7), 7, null);
    }

    private static GpsStatus.Result evaluate(Goal goal, GpsCurrentPosition pos, DebtSummaryResult debts) {
        return GpsStatusPolicy.evaluate(goal, pos, debts, AS_OF,
                GpsStatus.Policy.defaults(), GoalCalculationPolicy.defaults());
    }

    @Test
    void completedBeatsBlocked_zeroCapacityButGoalDone() {
        var goal = amountGoal("108000000.00", "108000000.00", AS_OF.plusMonths(5));
        var result = evaluate(goal, position("0.00"), openDebts());
        assertThat(result.status()).isEqualTo(GpsStatus.Status.COMPLETED);
    }

    @Test
    void zeroCapacityWithRemaining_isBlocked() {
        var goal = amountGoal("108000000.00", "20000000.00", AS_OF.plusMonths(5));
        var result = evaluate(goal, position("0.00"), openDebts());
        assertThat(result.status()).isEqualTo(GpsStatus.Status.BLOCKED);
        assertThat(((GpsStatus.Blocked) result.condition()).reasonCode())
                .isEqualTo("NO_AVAILABLE_CAPACITY");
    }

    @Test
    void missingProfile_isBlockedWithProfileReason() {
        var goal = amountGoal("108000000.00", "20000000.00", AS_OF.plusMonths(5));
        var result = evaluate(goal, position("0.00", true), openDebts());
        assertThat(result.status()).isEqualTo(GpsStatus.Status.BLOCKED);
        assertThat(((GpsStatus.Blocked) result.condition()).reasonCode())
                .isEqualTo("MISSING_FINANCIAL_PROFILE");
    }

    @Test
    void blockedPortfolio_beatsLateness() {
        var goal = amountGoal("108000000.00", "20000000.00", AS_OF.plusMonths(5));
        var blocked = debts(ProjectionStatus.BLOCKED, null, null, "PORTFOLIO_CONTAINS_BLOCKED_DEBTS");
        var result = evaluate(goal, position("31000000.00"), blocked);
        assertThat(result.status()).isEqualTo(GpsStatus.Status.BLOCKED);
    }

    @Test
    void onTrackWhenEtaWithinTarget() {
        // remaining 88M, capacity 31M → eta 3; 5 months remaining → lateness 0.
        var goal = amountGoal("108000000.00", "20000000.00", LocalDate.of(2027, 3, 1));
        var result = evaluate(goal, position("31000000.00"), openDebts());
        assertThat(result.status()).isEqualTo(GpsStatus.Status.ON_TRACK);
    }

    @Test
    void lateWithinTolerance_isAtRisk() {
        // eta 3 vs 2 months remaining → lateness 1 ≤ 3.
        var goal = amountGoal("108000000.00", "20000000.00", AS_OF.plusMonths(2));
        var result = evaluate(goal, position("31000000.00"), openDebts());
        assertThat(result.status()).isEqualTo(GpsStatus.Status.AT_RISK);
        assertThat(((GpsStatus.AtRisk) result.condition()).lateness()).isEqualTo(1);
    }

    @Test
    void lateBeyondTolerance_isOffTrack() {
        // eta 3 vs 0 remaining window... use far target in past: expired, full remainder due.
        var goal = amountGoal("108000000.00", "20000000.00", AS_OF.minusDays(1));
        var result = evaluate(goal, position("11000000.00"), openDebts());
        assertThat(result.status()).isEqualTo(GpsStatus.Status.OFF_TRACK);
    }

    @Test
    void undatedGoal_neverAtRiskOrOffTrack() {
        var goal = amountGoal("108000000.00", "20000000.00", null);
        var result = evaluate(goal, position("1000000.00"), openDebts());
        assertThat(result.status()).isEqualTo(GpsStatus.Status.ON_TRACK);
    }

    @Test
    void debtFreedom_completedIffPortfolioCompleted() {
        var goal = debtFreedomGoal(LocalDate.of(2027, 12, 31));
        var done = debts(ProjectionStatus.COMPLETED, AS_OF, 0, "DEBT_ALREADY_PAID");
        assertThat(evaluate(goal, position("31000000.00"), done).status())
                .isEqualTo(GpsStatus.Status.COMPLETED);
    }

    @Test
    void debtFreedom_reactivatesWhenNewDebtAppears() {
        // Same goal, but the portfolio now has months remaining: not COMPLETED anymore.
        var goal = debtFreedomGoal(LocalDate.of(2027, 12, 31));
        var result = evaluate(goal, position("31000000.00"), openDebts());
        assertThat(result.status()).isNotEqualTo(GpsStatus.Status.COMPLETED);
    }

    @Test
    void debtFreedom_amountsNeverDecideCompletion() {
        var goal = Goal.recorded("VND", "Debt Freedom", "DEBT_FREEDOM",
                "100000000.00", "120000000.00", LocalDate.of(2027, 12, 31), 1);
        var result = evaluate(goal, position("31000000.00"), openDebts());
        assertThat(result.status()).isNotEqualTo(GpsStatus.Status.COMPLETED);
    }
}
