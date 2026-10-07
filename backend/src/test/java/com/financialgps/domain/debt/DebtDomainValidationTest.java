package com.financialgps.domain.debt;

import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T001 RED: domain model + invariants (spec §4.1–§4.3, §12.1–§12.5). */
class DebtDomainValidationTest {

    private static Debt debt(String balance, String min, String planned, DebtStatus status) {
        return new Debt("Techcombank", DebtType.CREDIT_CARD,
                Money.of("20000000.00", "VND"), Money.of(balance, "VND"),
                Rate.of("0.180000"), Money.of(min, "VND"), Money.of(planned, "VND"),
                15, status);
    }

    @Test
    void activeDebtWithConsistentPaymentsIsAccepted() {
        Debt debt = debt("15000000.00", "1500000.00", "3000000.00", DebtStatus.ACTIVE);

        assertThat(debt.contributesToTotals()).isTrue();
        assertThat(debt.debtType()).isEqualTo(DebtType.CREDIT_CARD);
    }

    @Test
    void plannedBelowMinimumIsRejected() {
        assertThatThrownBy(() -> debt("15000000.00", "1500000.00", "1499999.99", DebtStatus.ACTIVE))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo("DEBT_PLANNED_BELOW_MINIMUM");
    }

    @Test
    void positiveBalanceRequiresPositivePayments() {
        assertThatThrownBy(() -> debt("15000000.00", "0.00", "3000000.00", DebtStatus.ACTIVE))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo("DEBT_POSITIVE_BALANCE_REQUIRES_PAYMENTS");

        assertThatThrownBy(() -> debt("15000000.00", "1500000.00", "0.00", DebtStatus.ACTIVE))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo("DEBT_POSITIVE_BALANCE_REQUIRES_PAYMENTS");
    }

    @Test
    void zeroBalanceMustBePaidOffWithZeroPayments() {
        Debt paid = debt("0.00", "0.00", "0.00", DebtStatus.PAID_OFF);

        assertThat(paid.contributesToTotals()).isFalse();

        assertThatThrownBy(() -> debt("0.00", "0.00", "0.00", DebtStatus.ACTIVE))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo("DEBT_STATUS_INCONSISTENT");

        assertThatThrownBy(() -> debt("0.00", "100.00", "100.00", DebtStatus.PAID_OFF))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo("DEBT_PAID_PAYMENTS_MUST_BE_ZERO");
    }

    @Test
    void negativeMoneyIsRejectedAtConstruction() {
        assertThatThrownBy(() -> Money.of("-1.00", "VND"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void negativeRateIsRejected() {
        assertThatThrownBy(() -> Rate.of("-0.010000"))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo("RATE_NEGATIVE");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 32, -5})
    void dueDayOutside1To31IsRejected(int dueDay) {
        assertThatThrownBy(() -> new Debt("Bank", DebtType.OTHER,
                Money.of("100.00", "VND"), Money.of("100.00", "VND"), Rate.zero(),
                Money.of("10.00", "VND"), Money.of("10.00", "VND"), dueDay, DebtStatus.ACTIVE))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).code())
                .isEqualTo("DEBT_DUE_DAY_INVALID");
    }

    @Test
    void paidOffTransitionZeroesPayments() {
        Debt active = debt("15000000.00", "1500000.00", "3000000.00", DebtStatus.ACTIVE);

        Debt paid = active.paidOff(Money.zero("VND"));

        assertThat(paid.status()).isEqualTo(DebtStatus.PAID_OFF);
        assertThat(paid.minimumPayment().asDecimalString()).isEqualTo("0.00");
        assertThat(paid.plannedPayment().asDecimalString()).isEqualTo("0.00");
    }

    @Test
    void archivedDebtsNeverContributeToTotals() {
        Debt active = debt("15000000.00", "1500000.00", "3000000.00", DebtStatus.ACTIVE);

        assertThat(active.archived().status()).isEqualTo(DebtStatus.ARCHIVED);
        assertThat(active.archived().contributesToTotals()).isFalse();
    }
}
