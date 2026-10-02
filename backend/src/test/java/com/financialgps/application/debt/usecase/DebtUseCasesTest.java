package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.application.debt.port.out.DebtRecord;
import com.financialgps.application.debt.port.out.DebtStore;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T008 RED: debt use cases over mocked output ports (no Spring, no DB). */
class DebtUseCasesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());

    private final DebtStore debts = mock(DebtStore.class);
    private final DebtIncomeReader incomes = mock(DebtIncomeReader.class);
    private final DebtBusinessDate dates = () -> TODAY;
    private final DebtUseCases useCases = new DebtUseCases(debts, incomes, dates);

    private static DebtRecord stored(UUID id, String balance, String min, String plan, String status) {
        return new DebtRecord(id, OWNER, "Bank", "CREDIT_CARD", balance, balance, "0.120000",
                min, plan, 15, status);
    }

    private static DebtModels.DebtCommand command(String balance, String min, String plan) {
        return new DebtModels.DebtCommand("Bank", "CREDIT_CARD", balance, balance, "0.120000",
                min, plan, 15);
    }

    @Test
    void recordAssignsServerIdAndProjects() {
        when(debts.save(any())).thenAnswer(i -> {
            DebtRecord r = i.getArgument(0);
            return new DebtRecord(UUID.randomUUID(), r.owner(), r.creditor(), r.debtType(),
                    r.originalPrincipal(), r.outstandingBalance(), r.annualInterestRate(),
                    r.minimumPayment(), r.plannedPayment(), r.dueDay(), r.status());
        });

        DebtModels.DebtView view = useCases.record(OWNER, command("1000.00", "50.00", "100.00"));

        assertThat(view.id()).isNotBlank();
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.projection().status()).isEqualTo("AVAILABLE");
        assertThat(view.projection().numberOfPayments()).isEqualTo(11);
    }

    @Test
    void plannedBelowMinimumFailsValidation() {
        assertThatThrownBy(() -> useCases.record(OWNER, command("1000.00", "100.00", "50.00")))
                .isInstanceOf(DebtValidationException.class)
                .extracting(e -> ((DebtValidationException) e).code())
                .isEqualTo("DEBT_PLANNED_BELOW_MINIMUM");
    }

    @Test
    void summaryAggregatesAndComputesDti() {
        when(debts.findAllByOwner(OWNER)).thenReturn(List.of(
                stored(UUID.randomUUID(), "1000.00", "50.00", "100.00", "ACTIVE"),
                stored(UUID.randomUUID(), "2000.00", "200.00", "200.00", "ACTIVE")));
        when(incomes.totalActiveMonthlyIncome(OWNER))
                .thenReturn(Optional.of(new BigDecimal("10000.00")));

        DebtModels.DebtSummaryView summary = useCases.summary(OWNER);

        assertThat(summary.totalOutstandingDebt()).isEqualTo("3000.00");
        assertThat(summary.totalMinimumMonthlyPayment()).isEqualTo("250.00");
        assertThat(summary.debtToIncome().status()).isEqualTo("AVAILABLE");
        assertThat(summary.debtToIncome().ratio()).isEqualTo("0.0250");
        assertThat(summary.portfolioProjection().status()).isEqualTo("AVAILABLE");
        assertThat(summary.asOf()).isEqualTo("2026-10-01");
    }

    @Test
    void summaryWithoutIncomeReportsDtiUnavailable() {
        when(debts.findAllByOwner(OWNER)).thenReturn(List.of(
                stored(UUID.randomUUID(), "1000.00", "100.00", "100.00", "ACTIVE")));
        when(incomes.totalActiveMonthlyIncome(OWNER)).thenReturn(Optional.empty());

        DebtModels.DebtSummaryView summary = useCases.summary(OWNER);

        assertThat(summary.debtToIncome().status()).isEqualTo("UNAVAILABLE");
        assertThat(summary.debtToIncome().reasonCode()).isEqualTo("ZERO_OR_MISSING_INCOME");
    }

    @Test
    void archivedOrMissingDebtReadsAs404() {
        UUID id = UUID.randomUUID();
        when(debts.findByIdAndOwner(id, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCases.get(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> useCases.delete(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
