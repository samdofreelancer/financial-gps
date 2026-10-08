package com.financialgps.domain.debt;

import com.financialgps.domain.model.DomainValidationException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rate bounds (spec 002 §4.1): a rate is a fraction, 0 <= rate <= 1, at most 6 decimals. */
class RateTest {

    @Test
    void acceptsBoundaryFractions() {
        assertThat(Rate.of("0.000000").asDecimalString()).isEqualTo("0.000000");
        assertThat(Rate.of("1.000000").asDecimalString()).isEqualTo("1.000000");
        assertThat(Rate.of("0.180000").asDecimalString()).isEqualTo("0.180000");
    }

    @Test
    void rejectsAboveOneHundredPercent() {
        assertThatThrownBy(() -> Rate.of("1.000001"))
                .isInstanceOf(DomainValidationException.class)
                .extracting(exception -> ((DomainValidationException) exception).code())
                .isEqualTo("RATE_EXCEEDS_MAXIMUM");
        assertThatThrownBy(() -> Rate.of("5.00"))
                .isInstanceOf(DomainValidationException.class)
                .extracting(exception -> ((DomainValidationException) exception).code())
                .isEqualTo("RATE_EXCEEDS_MAXIMUM");
    }

    @Test
    void rejectsNegativeAndOverPrecise() {
        assertThatThrownBy(() -> Rate.of("-0.01"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Rate.of("0.1234567"))
                .isInstanceOf(DomainValidationException.class);
    }
}
