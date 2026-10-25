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
 * 004 review hardening: blocker reason-code mapping must never throw. Reason codes are a shared
 * string vocabulary between GPS status, the debt portfolio projection and (in future) new debt
 * reasons. An unknown code must degrade to the generic portfolio blocker, not a
 * {@code Code.valueOf} exception that 500s the GPS endpoint.
 */
class GpsBlockerFactoryTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 11);

    private static Goal amountGoal() {
        return Goal.recorded("VND", "Emergency Fund", "EMERGENCY_FUND",
                "108000000.00", "20000000.00", AS_OF.plusMonths(5), 1);
    }

    private static GpsCurrentPosition position() {
        GpsCurrentPosition.Builder b = GpsCurrentPosition.builder();
        Money zero = Money.zero("VND");
        var prov = new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated("t", "t"));
        b.income(new GpsCurrentPosition.Available("income", zero, prov.value()));
        b.expense(new GpsCurrentPosition.Available("expense", zero, prov.value()));
        b.mandatoryPayment(new GpsCurrentPosition.Available("mandatoryPayment", zero, prov.value()));
        b.netCashFlow(new GpsCurrentPosition.Available("netCashFlow", zero, prov.value()));
        b.availableCapacity(new GpsCurrentPosition.Available("availableCapacity",
                Money.of("0.00", "VND"), prov.value()));
        b.savings(new GpsCurrentPosition.Available("savings", zero, prov.value()));
        b.emergencyFund(new GpsCurrentPosition.Available("emergencyFund", zero, prov.value()));
        b.dependents(0);
        b.totalOutstandingDebt(new GpsCurrentPosition.Available("totalOutstandingDebt", zero, prov.value()));
        b.dti(new GpsCurrentPosition.DtiValue("AVAILABLE", new BigDecimal("0.1000"), null, "dti", prov));
        return b.build();
    }

    private static DebtSummaryResult openDebts() {
        return new DebtSummaryResult(
                new BigDecimal("120000000.00"), new BigDecimal("20000000.00"),
                new BigDecimal("20000000.00"), null, "VND",
                new DebtSummaryResult.DtiResult("AVAILABLE", new BigDecimal("0.2703"), null, "dti"),
                new DebtSummaryResult.PortfolioProjectionResult(ProjectionStatus.AVAILABLE, AS_OF.plusMonths(7), 7,
                        new BigDecimal("1000000.00"), null, "payoff", List.of()),
                0, AS_OF);
    }

    private static GpsStatus.Result blocked(String reasonCode) {
        var prov = new GpsProvenance.UnavailableEntry(
                new GpsProvenance.Unavailable("availableCapacity", reasonCode, "explanation"));
        return new GpsStatus.Result(GpsStatus.Status.BLOCKED, "blocked",
                new GpsStatus.Blocked(reasonCode, "explanation", prov));
    }

    @Test
    void knownReasonCode_mapsToItsBlockerCode() {
        var blockers = GpsBlockerFactory.buildBlockers(amountGoal(), position(), openDebts(), AS_OF,
                blocked("NO_AVAILABLE_CAPACITY"), GoalCalculationPolicy.defaults());
        assertThat(blockers).hasSize(1);
        assertThat(blockers.get(0).code()).isEqualTo(GpsBlocker.Code.NO_AVAILABLE_CAPACITY);
    }

    @Test
    void unknownReasonCode_fallsBackToPortfolioBlocker_neverThrows() {
        var blockers = GpsBlockerFactory.buildBlockers(amountGoal(), position(), openDebts(), AS_OF,
                blocked("SOME_FUTURE_DEBT_REASON"), GoalCalculationPolicy.defaults());
        assertThat(blockers).hasSize(1);
        assertThat(blockers.get(0).code()).isEqualTo(GpsBlocker.Code.PORTFOLIO_CONTAINS_BLOCKED_DEBTS);
    }
}