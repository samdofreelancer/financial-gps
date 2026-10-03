package com.financialgps.api.debt;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.in.DeleteDebt;
import com.financialgps.application.debt.port.in.GetDebtSchedule;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.debt.port.in.GetDebts;
import com.financialgps.application.debt.port.in.RecordDebt;
import com.financialgps.application.debt.port.in.UpdateDebt;
import com.financialgps.platform.security.CurrentOwnerProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter only: binds/validates DTOs, resolves the actor, invokes input ports. */
@RestController
@RequestMapping("/api/v1/debts")
public class DebtController {

    private final RecordDebt recordDebt;
    private final UpdateDebt updateDebt;
    private final DeleteDebt deleteDebt;
    private final GetDebts getDebts;
    private final GetDebtSummary getDebtSummary;
    private final GetDebtSchedule getDebtSchedule;
    private final CurrentOwnerProvider owners;

    public DebtController(RecordDebt recordDebt, UpdateDebt updateDebt, DeleteDebt deleteDebt,
                          GetDebts getDebts, GetDebtSummary getDebtSummary,
                          GetDebtSchedule getDebtSchedule, CurrentOwnerProvider owners) {
        this.recordDebt = recordDebt;
        this.updateDebt = updateDebt;
        this.deleteDebt = deleteDebt;
        this.getDebts = getDebts;
        this.getDebtSummary = getDebtSummary;
        this.getDebtSchedule = getDebtSchedule;
        this.owners = owners;
    }

    @GetMapping
    public List<DebtModels.DebtView> list() {
        OwnerId owner = owners.requireCurrentOwner();
        return getDebts.list(owner);
    }

    @PostMapping
    public ResponseEntity<DebtModels.DebtView> create(
            @Valid @RequestBody DebtDtos.DebtRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        DebtModels.DebtView view = recordDebt.record(owner, new DebtModels.DebtCommand(
                request.creditor(), request.debtType(), request.originalPrincipal(),
                request.outstandingBalance(), request.annualInterestRate(),
                request.minimumPayment(), request.plannedPayment(), request.dueDay()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/{id}")
    public DebtModels.DebtView get(@PathVariable UUID id) {
        return getDebts.get(owners.requireCurrentOwner(), id);
    }

    @GetMapping("/summary")
    public DebtModels.DebtSummaryView summary() {
        return getDebtSummary.summary(owners.requireCurrentOwner());
    }

    /** Payment calendar: period, due date, principal, interest, ending balance per month. */
    @GetMapping("/{id}/schedule")
    public DebtModels.DebtScheduleView schedule(@PathVariable UUID id) {
        return getDebtSchedule.schedule(owners.requireCurrentOwner(), id);
    }

    @PutMapping("/{id}")
    public DebtModels.DebtView update(@PathVariable UUID id,
                                      @Valid @RequestBody DebtDtos.DebtRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return updateDebt.update(owner, id, new DebtModels.DebtUpdateCommand(
                request.creditor(), request.debtType(), request.originalPrincipal(),
                request.outstandingBalance(), request.annualInterestRate(),
                request.minimumPayment(), request.plannedPayment(), request.dueDay()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteDebt.delete(owners.requireCurrentOwner(), id);
        return ResponseEntity.noContent().build();
    }
}
