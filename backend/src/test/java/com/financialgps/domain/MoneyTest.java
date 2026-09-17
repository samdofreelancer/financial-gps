package com.financialgps.domain;

import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 001 TDD RED: Money value-object guard rails (calculation-rules §1 — decimal, never floating
 * point; negative rejected; single currency in v1).
 */
class MoneyTest {

    @Test
    void rejectsNegativeAmountsWithStableCode() {
        assertThatThrownBy(() -> Money.of("-0.01", "VND"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("negative")
                .extracting(exception -> ((DomainValidationException) exception).code())
                .isEqualTo("MONEY_NEGATIVE");
    }

    @Test
    void rejectsNonDecimalFormsInsteadOfGuessing() {
        for (String invalid : new String[]{"abc", "1E+2", "10,50", "1.2.3", " 10.00 "}) {
            assertThatThrownBy(() -> Money.of(invalid, "VND"))
                    .as("Money must reject %s", invalid)
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> Money.of("1.00", " "))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void keepsScaleTwoAndExactDecimalArithmetic() {
        Money sum = Money.of("19.99", "VND").add(Money.of("0.01", "VND"));

        assertThat(sum.asDecimalString()).isEqualTo("20.00");
        assertThat(sum.amount().scale()).isEqualTo(Money.SCALE);
    }

    @Test
    void decimalStringFormIsPlainAndNeverScientific() {
        assertThat(Money.of("99999999999999999.99", "VND").asDecimalString())
                .isEqualTo("99999999999999999.99");
        assertThat(Money.zero("VND").asDecimalString()).isEqualTo("0.00");
    }

    @Test
    void subtractionMayGoNegativeBecauseNetCashFlowIsReported() {
        Money negative = Money.of("20.00", "VND").subtract(Money.of("30.00", "VND"));

        assertThat(negative.asDecimalString()).isEqualTo("-10.00");
    }

    @Test
    void maxZeroClampsCapacityWithoutConcealingTheFlow() {
        Money negative = Money.of("20.00", "VND").subtract(Money.of("30.00", "VND"));

        assertThat(negative.maxZero()).isEqualTo(Money.zero("VND"));
        assertThat(negative.asDecimalString()).as("the negative Net Cash Flow itself is untouched")
                .isEqualTo("-10.00");
        assertThat(Money.of("44.00", "VND").maxZero().asDecimalString()).isEqualTo("44.00");
    }

    @Test
    void currencyConversionIsOutOfScopeForV1() {
        assertThatThrownBy(() -> Money.of("1.00", "VND").add(Money.of("1.00", "USD")))
                .isInstanceOf(DomainValidationException.class)
                .extracting(exception -> ((DomainValidationException) exception).code())
                .isEqualTo("CURRENCY_MISMATCH");
    }
}
