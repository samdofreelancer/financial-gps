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
 * 004: ETA is CEILING-based for amount goals, unavailable with a machine-readable
 * reason when no finite route exists, and read from the 002 portfolio for
 * debt-freedom destinations.
 */
class GpsEtaCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 11);

    private static Goal amountGoal(String target, String current, LocalDate targetDate) {
        return Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND", target, current, targetDate, 1);
    }

    private static GpsCurrentPosition position(String capacity, boolean missing) {
        var b = GpsCurrentPosition.builder();
        Money zero = Money.zero("VND");
        var prov = new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated("t", "t"));
        b.income(new GpsCurrentPosition.Available("income", zero, prov.value()));
        b.expense(new GpsCurrentPosition.Available("expense", zero, prov.value()));
        b.mandatoryPayment(new GpsCurrentPosition.Available("mandatoryPayment", zero, prov.value()));
        b.netCashFlow(new GpsCurrentPosition.Available("netCashFlow", zero, prov.value()));
        if (missing) {
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

    private static DebtSummaryResult debts(ProjectionStatus status, LocalDate date, Integer months) {
        return new DebtSummaryResult(
                new BigDecimal("120000000.00"), new BigDecimal("20000000.00"),
                new BigDecimal("20000000.00"), null, "VND",
                new DebtSummaryResult.DtiResult("AVAILABLE", new BigDecimal("0.2703"), null, "dti"),
                new DebtSummaryResult.PortfolioProjectionResult(status, date, months,
                        new BigDecimal("1000000.00"),
                        status == ProjectionStatus.BLOCKED ? "PORTFOLIO_CONTAINS_BLOCKED_DEBTS" : null,
                        status == ProjectionStatus.BLOCKED ? "blocked" : "payoff", List.of()),
                0, AS_OF);
    }

    @Test
    void amountEta_usesCeiling() {
        // 88M / 31M = 2.84 → 3 periods → 2027-01-11.
        var eta = GpsEtaCalculator.calculate(
                amountGoal("108000000.00", "20000000.00", LocalDate.of(2027, 3, 1)),
                position("31000000.00", false),
                debts(ProjectionStatus.AVAILABLE, AS_OF.plusMonths(7), 7),
                AS_OF, GoalCalculationPolicy.defaults());
        assertThat(eta.availability()).isEqualTo(GpsEta.Availability.CALCULATED);
        var calc = (GpsEta.CalculatedEta) eta;
        assertThat(calc.periods()).isEqualTo(3);
        assertThat(calc.date()).isEqualTo(LocalDate.of(2027, 1, 11));
    }

    @Test
    void completedGoal_etaIsAsOfWithZeroPeriods() {
        var eta = GpsEtaCalculator.calculate(
                amountGoal("108000000.00", "108000000.00", LocalDate.of(2027, 3, 1)),
                position("31000000.00", false),
                debts(ProjectionStatus.AVAILABLE, AS_OF.plusMonths(7), 7),
                AS_OF, GoalCalculationPolicy.defaults());
        var calc = (GpsEta.CalculatedEta) eta;
        assertThat(calc.periods()).isZero();
        assertThat(calc.date()).isEqualTo(AS_OF);
    }

    @Test
    void zeroCapacity_etaUnavailableWithReason() {
        var eta = GpsEtaCalculator.calculate(
                amountGoal("108000000.00", "20000000.00", LocalDate.of(2027, 3, 1)),
                position("0.00", false),
                debts(ProjectionStatus.AVAILABLE, AS_OF.plusMonths(7), 7),
                AS_OF, GoalCalculationPolicy.defaults());
        assertThat(eta.availability()).isEqualTo(GpsEta.Availability.UNAVAILABLE);
        assertThat(((GpsEta.UnavailableEta) eta).reason())
                .isEqualTo(GpsEta.Reason.NO_AVAILABLE_CAPACITY);
    }

    @Test
    void missingProfile_etaUnavailableWithReason() {
        var eta = GpsEtaCalculator.calculate(
                amountGoal("108000000.00", "20000000.00", LocalDate.of(2027, 3, 1)),
                position("0.00", true),
                debts(ProjectionStatus.AVAILABLE, AS_OF.plusMonths(7), 7),
                AS_OF, GoalCalculationPolicy.defaults());
        assertThat(((GpsEta.UnavailableEta) eta).reason())
                .isEqualTo(GpsEta.Reason.MISSING_FINANCIAL_PROFILE);
    }

    @Test
    void debtFreedom_etaComesFromPortfolio() {
        var goal = Goal.recorded("VND", "Debt Freedom", "DEBT_FREEDOM", null, null,
                LocalDate.of(2027, 12, 31), 1);
        var eta = GpsEtaCalculator.calculate(goal, position("31000000.00", false),
                debts(ProjectionStatus.AVAILABLE, LocalDate.of(2027, 5, 11), 7),
                AS_OF, GoalCalculationPolicy.defaults());
        var calc = (GpsEta.CalculatedEta) eta;
        assertThat(calc.date()).isEqualTo(LocalDate.of(2027, 5, 11));
        assertThat(calc.periods()).isEqualTo(7);
    }

    @Test
    void debtFreedom_blockedPortfolio_etaUnavailable() {
        var goal = Goal.recorded("VND", "Debt Freedom", "DEBT_FREEDOM", null, null,
                LocalDate.of(2027, 12, 31), 1);
        var eta = GpsEtaCalculator.calculate(goal, position("31000000.00", false),
                debts(ProjectionStatus.BLOCKED, null, null),
                AS_OF, GoalCalculationPolicy.defaults());
        assertThat(eta.availability()).isEqualTo(GpsEta.Availability.UNAVAILABLE);
    }
}
