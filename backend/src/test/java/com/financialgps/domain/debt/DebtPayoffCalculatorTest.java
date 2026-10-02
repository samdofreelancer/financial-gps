package com.financialgps.domain.debt;

import com.financialgps.domain.model.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

/** T002 RED: oracle Table 11.1 REF-D01–REF-D09 (spec §5 + §11.1). */
class DebtPayoffCalculatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);
    private static final DebtCalculationPolicy POLICY = DebtCalculationPolicy.defaults();

    private static Debt debt(String balance, String rate, String planned) {
        return new Debt("Creditor", DebtType.PERSONAL_LOAN,
                Money.of(balance, "VND"), Money.of(balance, "VND"),
                rate == null ? null : Rate.of(rate),
                Money.of("1.00", "VND"), Money.of(planned, "VND"), 15, DebtStatus.ACTIVE);
    }

    @Test
    void refD01_standardPositiveInterest() {
        var r = DebtPayoffCalculator.project(debt("1000.00", "0.120000", "50.00"), AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.AVAILABLE);
        assertThat(r.numberOfPayments()).isEqualTo(23);
        assertThat(r.projectedPayoffDate()).isEqualTo(LocalDate.of(2028, 9, 1));
        assertThat(r.finalPayment()).isEqualByComparingTo(new BigDecimal("21.36"));
        assertThat(r.totalInterest()).isEqualByComparingTo(new BigDecimal("121.36"));
    }

    @Test
    void refD02_zeroInterestExactDivisor() {
        var r = DebtPayoffCalculator.project(debt("1000.00", "0.000000", "100.00"), AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.AVAILABLE);
        assertThat(r.numberOfPayments()).isEqualTo(10);
        assertThat(r.projectedPayoffDate()).isEqualTo(LocalDate.of(2027, 8, 1));
        assertThat(r.finalPayment()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(r.totalInterest()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void refD03_zeroInterestFinalPaymentClamp() {
        var r = DebtPayoffCalculator.project(debt("1000.00", "0.000000", "300.00"), AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.AVAILABLE);
        assertThat(r.numberOfPayments()).isEqualTo(4);
        assertThat(r.projectedPayoffDate()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(r.finalPayment()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void refD04_overpaymentClamp() {
        var r = DebtPayoffCalculator.project(debt("50.00", "0.120000", "100.00"), AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.AVAILABLE);
        assertThat(r.numberOfPayments()).isEqualTo(1);
        assertThat(r.projectedPayoffDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(r.finalPayment()).isEqualByComparingTo(new BigDecimal("50.50"));
        assertThat(r.totalInterest()).isEqualByComparingTo(new BigDecimal("0.50"));
    }

    @Test
    void refD05_paymentBelowInterestIsBlocked() {
        var r = DebtPayoffCalculator.project(debt("1000.00", "0.120000", "8.00"), AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.BLOCKED);
        assertThat(r.reasonCode()).isEqualTo("PAYMENT_DOES_NOT_COVER_INTEREST");
        assertThat(r.projectedPayoffDate()).isNull();
        assertThat(r.numberOfPayments()).isNull();
        assertThat(r.totalInterest()).isNull();
        assertThat(r.finalPayment()).isNull();
    }

    @Test
    void refD06_paymentEqualsInterestIsBlocked() {
        var r = DebtPayoffCalculator.project(debt("1000.00", "0.120000", "10.00"), AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.BLOCKED);
        assertThat(r.reasonCode()).isEqualTo("PAYMENT_COVERS_ONLY_INTEREST");
        assertThat(r.projectedPayoffDate()).isNull();
    }

    @Test
    void refD07_missingRateNeverAssumesZero() {
        var r = DebtPayoffCalculator.project(debt("1000.00", null, "50.00"), AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.BLOCKED);
        assertThat(r.reasonCode()).isEqualTo("INTEREST_RATE_MISSING");
    }

    @Test
    void refD08_zeroBalanceIsCompleted() {
        var zero = new Debt("C", DebtType.PERSONAL_LOAN, Money.of("1000.00", "VND"),
                Money.of("0.00", "VND"), Rate.of("0.120000"), Money.of("0.00", "VND"),
                Money.of("0.00", "VND"), 15, DebtStatus.PAID_OFF);
        var r = DebtPayoffCalculator.project(zero, AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.COMPLETED);
        assertThat(r.reasonCode()).isEqualTo("DEBT_ALREADY_PAID");
        assertThat(r.numberOfPayments()).isZero();
        assertThat(r.projectedPayoffDate()).isEqualTo(AS_OF);
    }

    @Test
    void refD09_largeBalancePaymentEqualsInterestIsBlocked() {
        var big = new Debt("Bank", DebtType.CREDIT_CARD, Money.of("10000000.00", "VND"),
                Money.of("10000000.00", "VND"), Rate.of("0.180000"),
                Money.of("150000.00", "VND"), Money.of("150000.00", "VND"), 15, DebtStatus.ACTIVE);
        var r = DebtPayoffCalculator.project(big, AS_OF, POLICY);
        assertThat(r.status()).isEqualTo(ProjectionStatus.BLOCKED);
        assertThat(r.reasonCode()).isEqualTo("PAYMENT_COVERS_ONLY_INTEREST");
        assertThat(r.projectedPayoffDate()).isNull();
    }

    @Test
    void determinism_sameInputsSameAsOf_identicalResult() {
        var d = debt("1000.00", "0.120000", "50.00");
        assertThat(DebtPayoffCalculator.project(d, AS_OF, POLICY))
                .isEqualTo(DebtPayoffCalculator.project(d, AS_OF, POLICY));
    }
}
