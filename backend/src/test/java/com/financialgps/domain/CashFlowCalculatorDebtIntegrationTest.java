package com.financialgps.domain;

import com.financialgps.domain.engine.Assumptions;
import com.financialgps.domain.engine.FinancialEngine;
import com.financialgps.domain.engine.FinancialResult;
import com.financialgps.domain.finance.CashFlowCalculator;
import com.financialgps.domain.finance.CashFlowResult;
import com.financialgps.domain.model.Expense;
import com.financialgps.domain.model.Obligation;
import com.financialgps.domain.model.Portfolio;
import com.financialgps.domain.model.Income;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.policy.FinancialPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 002 T004 — Obligation -&gt; Financial Position integration (spec §8.2, SC5.1–SC5.3).
 *
 * <p>Pins the exact contract Feature 001 deferred to 002: {@code Mandatory Payment} is the sum
 * of obligations, it reduces Net Cash Flow one-for-one, and it clamps Available Capacity —
 * through the canonical {@code FinancialEngine} path, never a second calculator.
 *
 * <p>The engine sums every obligation it is given: which debts qualify (ACTIVE only) is the
 * debt→profile adapter's responsibility, covered by the HTTP journey
 * ({@code DebtFinancialPositionJourneyTest}, archived debts leave the position).
 */
class CashFlowCalculatorDebtIntegrationTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 1);
    private static final FinancialPolicy POLICY = FinancialPolicy.defaults();

    private static Income income(String amount) {
        return new Income(Money.of(amount, "VND"), "salary", true, AS_OF);
    }

    private static Expense expense(String amount) {
        return new Expense(Money.of(amount, "VND"), "living", Expense.ExpenseType.FIXED, true, AS_OF);
    }

    private static Obligation obligation(String minimum) {
        return new Obligation(Money.of(minimum, "VND"));
    }

    @Test
    void sc51_mandatoryPaymentFeedsNetCashFlowAndCapacity() {
        Portfolio input = new Portfolio(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(obligation("20000000.00")),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("24000000.00");
        assertThat(result.availableCapacity().asDecimalString()).isEqualTo("24000000.00");
    }

    @Test
    void sc53_onlyMinimumPaymentIsMandatory_notThePlannedSurplus() {
        Portfolio input = new Portfolio(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                // The adapter maps only the non-negotiable minimum: a planned surplus above it
                // never reaches baseline cash flow.
                List.of(obligation("20000000.00")),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("24000000.00");
    }

    @Test
    void noObligationsMeansNoMandatoryPayment() {
        Portfolio input = new Portfolio(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("0.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("44000000.00");
    }

    @Test
    void multipleObligationsSumTheirMinimums() {
        Portfolio input = new Portfolio(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(obligation("12000000.00"),
                        obligation("8000000.00")),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("24000000.00");
    }

    @Test
    void enginePathCarriesTheSameMandatoryPaymentAndExplainsIt() {
        Portfolio input = new Portfolio(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(obligation("20000000.00")),
                List.of());

        FinancialResult result = FinancialEngine.calculate(input, Assumptions.none(), AS_OF, POLICY);

        assertThat(result.position()).isEqualTo(CashFlowCalculator.calculate(input, AS_OF, POLICY));
        assertThat(result.position().mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(result.provenance()).anySatisfy(entry -> {
            assertThat(entry.field()).isEqualTo("Mandatory Payment");
            assertThat(entry.detail()).contains("ACTIVE debts minimumPayment");
        });
    }
}
