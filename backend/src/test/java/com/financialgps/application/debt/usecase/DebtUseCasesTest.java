package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.domain.debt.DebtStatus;
import com.financialgps.domain.debt.DebtStore;
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
    private final DebtCurrency currency = mock(DebtCurrency.class);
    private final DebtIncomeReader incomes = mock(DebtIncomeReader.class);
    private final DebtBusinessDate dates = () -> TODAY;
    private final RecordDebtUseCase record = new RecordDebtUseCase(debts, currency, dates);
    private final UpdateDebtUseCase update = new UpdateDebtUseCase(debts, currency, dates);
    private final DeleteDebtUseCase delete = new DeleteDebtUseCase(debts);
    private final GetDebtsUseCase getDebts = new GetDebtsUseCase(debts, currency, dates);
    private final GetDebtScheduleUseCase schedule = new GetDebtScheduleUseCase(debts, currency, dates);
    private final GetDebtSummaryUseCase summary = new GetDebtSummaryUseCase(debts, currency, incomes, dates);
    private final MarkDebtPaymentUseCase marks = new MarkDebtPaymentUseCase(debts, currency, dates);

    private static Debt stored(UUID id, String balance, String min, String plan, String status) {
        return Debt.reconstitute(DebtId.of(id), Debt.DEFAULT_CURRENCY, "Bank", "CREDIT_CARD",
                balance, balance, "0.120000", min, plan, 15, DebtStatus.valueOf(status), null);
    }

    private static DebtModels.DebtCommand command(String balance, String min, String plan) {
        return new DebtModels.DebtCommand("Bank", "CREDIT_CARD", balance, balance, "0.120000",
                min, plan, 15);
    }

    private static DebtModels.DebtUpdateCommand updateCommand(String balance, String min, String plan) {
        return new DebtModels.DebtUpdateCommand("Bank", "CREDIT_CARD", balance, balance, "0.120000",
                min, plan, 15);
    }

    @Test
    void recordAssignsServerIdAndProjects() {
        when(currency.currencyOf(OWNER)).thenReturn("VND");
        when(debts.save(any(), any())).thenAnswer(i -> {
            Debt d = i.getArgument(1);
            return d.id() == null ? d.withId(DebtId.of(UUID.randomUUID())) : d;
        });

        DebtModels.DebtView view = record.record(OWNER, command("1000.00", "50.00", "100.00"));

        assertThat(view.id()).isNotBlank();
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.projection().status()).isEqualTo("AVAILABLE");
        assertThat(view.projection().numberOfPayments()).isEqualTo(11);
    }

    @Test
    void recordInheritsPositionCurrency() {
        when(currency.currencyOf(OWNER)).thenReturn("USD");
        when(debts.save(any(), any())).thenAnswer(i -> {
            Debt d = i.getArgument(1);
            return d.id() == null ? d.withId(DebtId.of(UUID.randomUUID())) : d;
        });

        DebtModels.DebtView view = record.record(OWNER, command("1000.00", "50.00", "100.00"));

        assertThat(view.currency()).isEqualTo("USD");
    }

    @Test
    void plannedBelowMinimumFailsValidation() {
        when(currency.currencyOf(OWNER)).thenReturn("VND");
        assertThatThrownBy(() -> record.record(OWNER, command("1000.00", "100.00", "50.00")))
                .isInstanceOf(DebtValidationException.class)
                .extracting(e -> ((DebtValidationException) e).code())
                .isEqualTo("DEBT_PLANNED_BELOW_MINIMUM");
    }

    @Test
    void updateOfMissingDebtReadsAs404() {
        UUID id = UUID.randomUUID();
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> update.update(OWNER, id, updateCommand("1000.00", "50.00", "100.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Lifecycle is derived from the balance on every write (spec §4.3): updating a PAID_OFF debt to
     * a positive balance with positive payments deliberately re-opens it as ACTIVE. Pinned so the
     * resurrection is a conscious contract, not an accident.
     */
    @Test
    void updateReopensPaidOffDebtAsActiveWhenBalanceGoesPositive() {
        UUID id = UUID.randomUUID();
        Debt paidOff = Debt.reconstitute(DebtId.of(id), Debt.DEFAULT_CURRENCY, "Bank", "CREDIT_CARD",
                "1000.00", "0.00", "0.120000", "0.00", "0.00", 15, DebtStatus.PAID_OFF, null);
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.of(paidOff));
        when(currency.currencyOf(OWNER)).thenReturn("VND");
        when(debts.save(any(), any())).thenAnswer(i -> i.getArgument(1));

        DebtModels.DebtView view = update.update(OWNER, id, updateCommand("1000.00", "50.00", "100.00"));

        assertThat(view.status()).isEqualTo("ACTIVE");
    }

    @Test
    void summaryAggregatesAndComputesDti() {
        when(debts.findAllByOwner(OWNER)).thenReturn(List.of(
                stored(UUID.randomUUID(), "1000.00", "50.00", "100.00", "ACTIVE"),
                stored(UUID.randomUUID(), "2000.00", "200.00", "200.00", "ACTIVE")));
        when(currency.currencyOf(OWNER)).thenReturn("VND");
        when(incomes.totalActiveMonthlyIncome(OWNER))
                .thenReturn(Optional.of(new BigDecimal("10000.00")));

        DebtModels.DebtSummaryView summaryView = summary.summary(OWNER);

        assertThat(summaryView.totalOutstandingDebt()).isEqualTo("3000.00");
        assertThat(summaryView.totalMinimumMonthlyPayment()).isEqualTo("250.00");
        assertThat(summaryView.debtToIncome().status()).isEqualTo("AVAILABLE");
        assertThat(summaryView.debtToIncome().ratio()).isEqualTo("0.0250");
        assertThat(summaryView.portfolioProjection().status()).isEqualTo("AVAILABLE");
        assertThat(summaryView.asOf()).isEqualTo("2026-10-01");
    }

    @Test
    void summaryWithoutIncomeReportsDtiUnavailable() {
        when(debts.findAllByOwner(OWNER)).thenReturn(List.of(
                stored(UUID.randomUUID(), "1000.00", "100.00", "100.00", "ACTIVE")));
        when(currency.currencyOf(OWNER)).thenReturn("VND");
        when(incomes.totalActiveMonthlyIncome(OWNER)).thenReturn(Optional.empty());

        DebtModels.DebtSummaryView summaryView = summary.summary(OWNER);

        assertThat(summaryView.debtToIncome().status()).isEqualTo("UNAVAILABLE");
        assertThat(summaryView.debtToIncome().reasonCode()).isEqualTo("ZERO_OR_MISSING_INCOME");
    }

    @Test
    void scheduleReturnsRowsReconcilingWithProjection() {
        UUID id = UUID.randomUUID();
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.of(
                stored(id, "1000.00", "50.00", "100.00", "ACTIVE")));
        when(currency.currencyOf(OWNER)).thenReturn("VND");

        DebtModels.DebtScheduleView scheduleView = schedule.schedule(OWNER, id);

        assertThat(scheduleView.status()).isEqualTo("AVAILABLE");
        assertThat(scheduleView.currency()).isEqualTo("VND");
        assertThat(scheduleView.rows()).hasSize(scheduleView.numberOfPayments());
        // 1000 @ 12%/yr: first month interest 10.00, so principal 90.00 and balance 910.00.
        var first = scheduleView.rows().get(0);
        assertThat(first.period()).isEqualTo(1);
        assertThat(first.dueDate()).isEqualTo("2026-10-15");
        assertThat(first.interest()).isEqualTo("10.00");
        assertThat(first.principal()).isEqualTo("90.00");
        assertThat(first.endingBalance()).isEqualTo("910.00");
        // The calendar closes the loan: final row ends at zero and matches the card's projection.
        var last = scheduleView.rows().get(scheduleView.rows().size() - 1);
        assertThat(last.endingBalance()).isEqualTo("0.00");
        assertThat(last.payment()).isEqualTo(scheduleView.finalPayment());
    }

    @Test
    void scheduleOfMissingDebtReadsAs404() {
        UUID id = UUID.randomUUID();
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedule.schedule(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void manualPaymentMarkIsCurrentPeriodOnlyAndCanBeUndone() {
        UUID id = UUID.randomUUID();
        Debt unmarked = stored(id, "1000.00", "50.00", "100.00", "ACTIVE");
        Debt marked = Debt.reconstitute(DebtId.of(id), Debt.DEFAULT_CURRENCY, "Bank", "CREDIT_CARD",
                "1000.00", "1000.00", "0.120000", "50.00", "100.00", 15, DebtStatus.ACTIVE, TODAY);
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.of(unmarked));
        when(currency.currencyOf(OWNER)).thenReturn("VND");
        when(debts.save(any(), any())).thenAnswer(i -> i.getArgument(1));

        DebtModels.DebtView paid = marks.markPaid(OWNER, id);

        assertThat(paid.paidThisPeriod()).isTrue();
        assertThat(paid.outstandingBalance()).isEqualTo("1000.00");

        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.of(marked));
        DebtModels.DebtView undone = marks.undoMark(OWNER, id);

        assertThat(undone.paidThisPeriod()).isFalse();
        assertThat(undone.outstandingBalance()).isEqualTo("1000.00");
    }

    @Test
    void paymentMarkForPreviousMonthDoesNotCountForCurrentPeriod() {
        UUID id = UUID.randomUUID();
        Debt markedLastMonth = Debt.reconstitute(DebtId.of(id), Debt.DEFAULT_CURRENCY, "Bank",
                "CREDIT_CARD", "1000.00", "1000.00", "0.120000", "50.00", "100.00", 15,
                DebtStatus.ACTIVE, TODAY.minusMonths(1));
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.of(markedLastMonth));
        when(currency.currencyOf(OWNER)).thenReturn("VND");

        assertThat(getDebts.get(OWNER, id).paidThisPeriod()).isFalse();
    }

    @Test
    void archivedOrMissingDebtReadsAs404() {
        UUID id = UUID.randomUUID();
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getDebts.get(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> delete.delete(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lostArchiveRaceReadsAs404() {
        UUID id = UUID.randomUUID();
        when(debts.findByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(Optional.of(
                stored(id, "1000.00", "50.00", "100.00", "ACTIVE")));
        when(debts.archiveByIdAndOwner(DebtId.of(id), OWNER)).thenReturn(false);

        assertThatThrownBy(() -> delete.delete(OWNER, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
