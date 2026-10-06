package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.DeleteDebt;
import com.financialgps.application.debt.port.in.GetDebtSchedule;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.debt.port.in.GetDebts;
import com.financialgps.application.debt.port.in.MarkDebtPayment;
import com.financialgps.application.debt.port.in.RecordDebt;
import com.financialgps.application.debt.port.in.UpdateDebt;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.DomainValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Debt application service (plan §2.2): orchestrates persistence, business date and pure domain
 * calculation. No financial formula lives here — mapping + projection assembly only.
 */
public final class DebtUseCases implements RecordDebt, UpdateDebt, DeleteDebt, GetDebts,
    GetDebtSchedule, GetDebtSummary, MarkDebtPayment {

    private final DebtStore debts;
    private final DebtIncomeReader incomes;
    private final DebtBusinessDate dates;

    public DebtUseCases(DebtStore debts, DebtIncomeReader incomes, DebtBusinessDate dates) {
        this.debts = debts;
        this.incomes = incomes;
        this.dates = dates;
    }

    @Override
    public DebtModels.DebtView record(OwnerId owner, DebtModels.DebtCommand command) {
        Debt saved = debts.save(owner, toDomain(command.creditor(), command.debtType(),
                command.originalPrincipal(), command.outstandingBalance(), command.annualInterestRate(),
                command.minimumPayment(), command.plannedPayment(), command.dueDay()));
        return DebtViews.view(saved, dates.today());
    }

    @Override
    public DebtModels.DebtView update(OwnerId owner, UUID id, DebtModels.DebtUpdateCommand command) {
        Debt existing = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        Debt updated = toDomain(command.creditor(), command.debtType(), command.originalPrincipal(),
                command.outstandingBalance(), command.annualInterestRate(), command.minimumPayment(),
                command.plannedPayment(), command.dueDay())
                .withId(existing.id())
                .withPaymentMarkedOn(existing.paymentMarkedOn());
        return DebtViews.view(debts.save(owner, updated), dates.today());
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        debts.findByIdAndOwner(DebtId.of(id), owner).orElseThrow(ResourceNotFoundException::new);
        debts.archiveByIdAndOwner(DebtId.of(id), owner);
    }

    @Override
    public DebtModels.DebtView get(OwnerId owner, UUID id) {
        Debt debt = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        return DebtViews.view(debt, dates.today());
    }

    @Override
    public List<DebtModels.DebtView> list(OwnerId owner) {
        LocalDate asOf = dates.today();
        List<DebtModels.DebtView> views = new ArrayList<>();
        for (Debt debt : debts.findAllByOwner(owner)) {
            views.add(DebtViews.view(debt, asOf));
        }
        return List.copyOf(views);
    }

    @Override
    public DebtModels.DebtScheduleView schedule(OwnerId owner, UUID id) {
        Debt debt = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        return DebtViews.scheduleView(debt, dates.today());
    }

    @Override
    public DebtModels.DebtView markPaid(OwnerId owner, UUID id) {
        return setPaymentMark(owner, id, dates.today());
    }

    @Override
    public DebtModels.DebtView undoMark(OwnerId owner, UUID id) {
        return setPaymentMark(owner, id, null);
    }

    private DebtModels.DebtView setPaymentMark(OwnerId owner, UUID id, LocalDate markedOn) {
        Debt current = debts.findByIdAndOwner(DebtId.of(id), owner)
                .orElseThrow(ResourceNotFoundException::new);
        if (current.status() != com.financialgps.domain.debt.DebtStatus.ACTIVE) {
            throw new DebtValidationException("DEBT_NOT_ACTIVE", "Only active debts can be marked paid.");
        }
        return DebtViews.view(debts.save(owner, current.withPaymentMarkedOn(markedOn)), dates.today());
    }

    @Override
    public DebtModels.DebtSummaryView summary(OwnerId owner) {
        List<Debt> active = debts.findAllByOwner(owner).stream()
                .filter(Debt::contributesToTotals)
                .toList();
        return DebtViews.summaryView(active, incomes.totalActiveMonthlyIncome(owner).orElse(null),
                dates.today());
    }

    private static Debt toDomain(String creditor, String debtType, String originalPrincipal,
                                 String outstandingBalance, String annualInterestRate,
                                 String minimumPayment, String plannedPayment, Integer dueDay) {
        try {
            return Debt.recorded(Debt.DEFAULT_CURRENCY, creditor, debtType, originalPrincipal,
                    outstandingBalance, annualInterestRate, minimumPayment, plannedPayment, dueDay);
        } catch (DomainValidationException e) {
            throw new DebtValidationException(e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DebtValidationException("DEBT_INVALID_TYPE", "Unknown debt type: " + debtType);
        }
    }
}
