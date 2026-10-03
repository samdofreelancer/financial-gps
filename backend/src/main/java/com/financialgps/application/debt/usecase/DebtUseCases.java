package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
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
import com.financialgps.application.debt.port.out.DebtRecord;
import com.financialgps.application.debt.port.out.DebtStore;
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
        DebtRecord saved = debts.save(DebtMapping.toRecord(null, owner, command));
        return DebtViews.view(saved, dates.today());
    }

    @Override
    public DebtModels.DebtView update(OwnerId owner, UUID id, DebtModels.DebtUpdateCommand command) {
        debts.findByIdAndOwner(id, owner).orElseThrow(ResourceNotFoundException::new);
        DebtRecord saved = debts.save(DebtMapping.toRecord(id, owner, command));
        return DebtViews.view(saved, dates.today());
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        debts.findByIdAndOwner(id, owner).orElseThrow(ResourceNotFoundException::new);
        debts.archiveByIdAndOwner(id, owner);
    }

    @Override
    public DebtModels.DebtView get(OwnerId owner, UUID id) {
        DebtRecord record = debts.findByIdAndOwner(id, owner).orElseThrow(ResourceNotFoundException::new);
        return DebtViews.view(record, dates.today());
    }

    @Override
    public List<DebtModels.DebtView> list(OwnerId owner) {
        LocalDate asOf = dates.today();
        List<DebtModels.DebtView> views = new ArrayList<>();
        for (DebtRecord record : debts.findAllByOwner(owner)) {
            views.add(DebtViews.view(record, asOf));
        }
        return List.copyOf(views);
    }

    @Override
    public DebtModels.DebtScheduleView schedule(OwnerId owner, UUID id) {
        DebtRecord record = debts.findByIdAndOwner(id, owner).orElseThrow(ResourceNotFoundException::new);
        return DebtViews.scheduleView(record, dates.today());
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
        DebtRecord current = debts.findByIdAndOwner(id, owner)
                .orElseThrow(ResourceNotFoundException::new);
        if (!"ACTIVE".equals(current.status())) {
            throw new DebtValidationException("DEBT_NOT_ACTIVE", "Only active debts can be marked paid.");
        }
        return DebtViews.view(debts.setPaymentMarkedOn(id, owner, markedOn), dates.today());
    }

    @Override
    public DebtModels.DebtSummaryView summary(OwnerId owner) {
        return DebtViews.summaryView(DebtMapping.toDebts(debts.findAllByOwner(owner)),
                incomes.totalActiveMonthlyIncome(owner).orElse(null), dates.today());
    }
}
