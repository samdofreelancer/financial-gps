package com.financialgps.domain;

import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStatus;
import com.financialgps.domain.engine.Assumptions;
import com.financialgps.domain.engine.FinancialEngine;
import com.financialgps.domain.engine.FinancialResult;
import com.financialgps.domain.finance.CashFlowCalculator;
import com.financialgps.domain.finance.CashFlowResult;
import com.financialgps.domain.model.Expense;
import com.financialgps.domain.model.FinancialInput;
import com.financialgps.domain.model.Income;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.policy.FinancialPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 002 T004 — Debt -> Financial Position integration (spec §8.2, SC5.1–SC5.3).
 *
 * <p>Pins the exact contract Feature 001 deferred to 002: {@code Mandatory Payment} is the sum of
 * ACTIVE debts' {@code minimumPayment}, it reduces Net Cash Flow one-for-one, and it clamps
 * Available Capacity — through the canonical {@code FinancialEngine} path, never a second
 * calculator.
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

    private static Debt debt(String balance, String minimum, String planned, DebtStatus status) {
        return Debt.reconstitute("VND", "Bank", "CREDIT_CARD", null, balance, "0.000000",
                minimum, planned, 15, status);
    }

    @Test
    void sc51_mandatoryPaymentFeedsNetCashFlowAndCapacity() {
        FinancialInput input = new FinancialInput(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(debt("15000000.00", "20000000.00", "20000000.00", DebtStatus.ACTIVE)),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("24000000.00");
        assertThat(result.availableCapacity().asDecimalString()).isEqualTo("24000000.00");
    }

    @Test
    void sc53_onlyMinimumPaymentIsMandatory_notThePlannedSurplus() {
        FinancialInput input = new FinancialInput(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                // Planned 30M > minimum 20M: only the non-negotiable 20M may hit baseline cash flow.
                List.of(debt("15000000.00", "20000000.00", "30000000.00", DebtStatus.ACTIVE)),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("24000000.00");
    }

    @Test
    void archivedAndPaidOffDebtsNeverReduceCashFlow() {
        FinancialInput input = new FinancialInput(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(debt("15000000.00", "20000000.00", "20000000.00", DebtStatus.ACTIVE)
                                .archived(),
                        debt("0.00", "0.00", "0.00", DebtStatus.PAID_OFF)),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("0.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("44000000.00");
    }

    @Test
    void multipleActiveDebtsSumTheirMinimums() {
        FinancialInput input = new FinancialInput(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(debt("15000000.00", "12000000.00", "12000000.00", DebtStatus.ACTIVE),
                        debt("8000000.00", "8000000.00", "8000000.00", DebtStatus.ACTIVE)),
                List.of());

        CashFlowResult result = CashFlowCalculator.calculate(input, AS_OF, POLICY);

        assertThat(result.mandatoryPayment().asDecimalString()).isEqualTo("20000000.00");
        assertThat(result.netCashFlow().asDecimalString()).isEqualTo("24000000.00");
    }

    @Test
    void enginePathCarriesTheSameMandatoryPaymentAndExplainsIt() {
        FinancialInput input = new FinancialInput(
                List.of(income("74000000.00")),
                List.of(expense("30000000.00")),
                List.of(debt("15000000.00", "20000000.00", "20000000.00", DebtStatus.ACTIVE)),
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