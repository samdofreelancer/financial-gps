package com.financialgps.domain.debt;

import com.financialgps.domain.model.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Payment-schedule calendar: rows must reconcile digit-for-digit with the payoff projection
 * (same loop), and row dates must honour the contractual due day.
 */
class DebtScheduleTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);
    private static final DebtCalculationPolicy POLICY = DebtCalculationPolicy.defaults();

    private static Debt debt(String balance, String rate, String planned, Integer dueDay) {
        return new Debt("Creditor", DebtType.PERSONAL_LOAN,
                Money.of(balance, "VND"), Money.of(balance, "VND"),
                rate == null ? null : Rate.of(rate),
                Money.of("1.00", "VND"), Money.of(planned, "VND"), dueDay, DebtStatus.ACTIVE);
    }

    @Test
    void availableSchedule_reconcilesWithProjection() {
        Debt d = debt("1000.00", "0.120000", "50.00", null);
        DebtScheduleResult s = DebtPayoffCalculator.schedule(d, AS_OF, POLICY);
        DebtProjectionResult p = DebtPayoffCalculator.project(d, AS_OF, POLICY);

        assertThat(s.projection()).isEqualTo(p);
        assertThat(s.rows()).hasSize(p.numberOfPayments());

        // Row 1: interest = round(1000 * 0.12 / 12) = 10.00, principal = 50 - 10 = 40.00.
        var first = s.rows().get(0);
        assertThat(first.period()).isEqualTo(1);
        assertThat(first.interest()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(first.principal()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(first.endingBalance()).isEqualByComparingTo(new BigDecimal("960.00"));

        // The schedule closes the loan exactly: last balance 0, payments/principal add up.
        var last = s.rows().get(s.rows().size() - 1);
        assertThat(last.endingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(last.payment()).isEqualByComparingTo(p.finalPayment());
        BigDecimal totalPrincipal = s.rows().stream().map(DebtScheduleEntry::principal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = s.rows().stream().map(DebtScheduleEntry::interest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipal).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(totalInterest).isEqualByComparingTo(p.totalInterest());

        // No due day: dates follow the projection rule asOf + n months (oracle REF-D01).
        assertThat(first.dueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(last.dueDate()).isEqualTo(p.projectedPayoffDate());
    }

    @Test
    void rowDatesHonourDueDay() {
        Debt d = debt("1000.00", "0.120000", "50.00", 28);
        DebtScheduleResult s = DebtPayoffCalculator.schedule(d, AS_OF, POLICY);
        // AS_OF is 01/10/2026, before the 28th: October is still upcoming, so period 1 must stay
        // in October instead of jumping straight to November.
        assertThat(s.rows().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(s.rows().get(1).dueDate()).isEqualTo(LocalDate.of(2026, 11, 28));
        // Rows land on the contractual day; the projection payoff keeps the oracle rule
        // asOf + N months (REF-D01), so the calendar ends on or before it.
        var last = s.rows().get(s.rows().size() - 1);
        assertThat(last.dueDate()).isEqualTo(LocalDate.of(2028, 8, 28));
        assertThat(s.projection().projectedPayoffDate()).isEqualTo(LocalDate.of(2028, 9, 1));
        assertThat(last.dueDate()).isBeforeOrEqualTo(s.projection().projectedPayoffDate());
    }

    @Test
    void firstPeriodIsTheNextUpcomingDueDate() {
        Debt d = debt("1000.00", "0.120000", "50.00", 28);
        // Before the due day: the schedule starts this month (regression: October must not be
        // skipped just because asOf already sits in October).
        var beforeDue = DebtPayoffCalculator.schedule(d, LocalDate.of(2026, 10, 3), POLICY);
        assertThat(beforeDue.rows().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 10, 28));
        // On the due day the payment is still upcoming, so it stays period 1.
        var onDueDay = DebtPayoffCalculator.schedule(d, LocalDate.of(2026, 10, 28), POLICY);
        assertThat(onDueDay.rows().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 10, 28));
        // After the due day has passed, the calendar starts next month.
        var afterDue = DebtPayoffCalculator.schedule(d, LocalDate.of(2026, 10, 29), POLICY);
        assertThat(afterDue.rows().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    @Test
    void dueDayClampsToShortMonths() {
        Debt d = debt("1000.00", "0.120000", "50.00", 31);
        DebtScheduleResult s = DebtPayoffCalculator.schedule(d, LocalDate.of(2027, 12, 1), POLICY);
        assertThat(s.rows().get(0).dueDate()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(s.rows().get(1).dueDate()).isEqualTo(LocalDate.of(2028, 1, 31));
        assertThat(s.rows().get(2).dueDate()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void blockedDebtHasNoRows() {
        DebtScheduleResult below = DebtPayoffCalculator.schedule(
                debt("1000.00", "0.120000", "8.00", 15), AS_OF, POLICY);
        assertThat(below.projection().status()).isEqualTo(ProjectionStatus.BLOCKED);
        assertThat(below.rows()).isEmpty();

        DebtScheduleResult missing = DebtPayoffCalculator.schedule(
                debt("1000.00", null, "50.00", 15), AS_OF, POLICY);
        assertThat(missing.projection().status()).isEqualTo(ProjectionStatus.BLOCKED);
        assertThat(missing.rows()).isEmpty();
    }

    @Test
    void paidOffDebtHasNoRows() {
        var zero = new Debt("C", DebtType.PERSONAL_LOAN, Money.of("1000.00", "VND"),
                Money.of("0.00", "VND"), Rate.of("0.120000"), Money.of("0.00", "VND"),
                Money.of("0.00", "VND"), 15, DebtStatus.PAID_OFF);
        DebtScheduleResult s = DebtPayoffCalculator.schedule(zero, AS_OF, POLICY);
        assertThat(s.projection().status()).isEqualTo(ProjectionStatus.COMPLETED);
        assertThat(s.rows()).isEmpty();
    }
}
