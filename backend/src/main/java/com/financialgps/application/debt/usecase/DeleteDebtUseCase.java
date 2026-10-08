package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.debt.port.in.DeleteDebt;
import com.financialgps.domain.debt.DebtId;
import com.financialgps.domain.debt.DebtStore;
import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

/** Soft-delete a debt to ARCHIVED. A lost archive race answers 404, never a misleading 204. */
public final class DeleteDebtUseCase implements DeleteDebt {

    private final DebtStore debts;

    public DeleteDebtUseCase(DebtStore debts) {
        this.debts = debts;
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        debts.findByIdAndOwner(DebtId.of(id), owner).orElseThrow(ResourceNotFoundException::new);
        if (!debts.archiveByIdAndOwner(DebtId.of(id), owner)) {
            throw new ResourceNotFoundException();
        }
    }
}
