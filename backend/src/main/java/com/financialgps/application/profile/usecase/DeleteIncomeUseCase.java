package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.port.in.DeleteIncome;
import com.financialgps.application.profile.port.out.IncomeStore;

import java.util.UUID;

/**
 * Hard-delete an income line. The delete is owner-scoped in one statement, so a cross-owner id and a
 * missing id produce the identical outcome without a read-then-write race (FR-010).
 */
public final class DeleteIncomeUseCase implements DeleteIncome {

    private final IncomeStore incomes;

    public DeleteIncomeUseCase(IncomeStore incomes) {
        this.incomes = incomes;
    }

    @Override
    public void delete(OwnerId owner, UUID id) {
        if (!incomes.delete(id, owner)) {
            throw new ResourceNotFoundException();
        }
    }
}
