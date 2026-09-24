package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.UpdateIncome;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;

import java.util.UUID;

/**
 * Replace an income line's editable fields. Only amount and source are editable: identity, owner,
 * profile, active flag and effective date are preserved, so an edit can never re-home or back-date a
 * line. A cross-owner id yields the same outcome as a missing one (FR-010).
 */
public final class UpdateIncomeUseCase implements UpdateIncome {

    private final IncomeStore incomes;

    public UpdateIncomeUseCase(IncomeStore incomes) {
        this.incomes = incomes;
    }

    @Override
    public ProfileModels.IncomeView update(OwnerId owner, UUID id, ProfileModels.IncomeCommand command) {
        IncomeRecord current = incomes.findById(id, owner).orElseThrow(ResourceNotFoundException::new);
        IncomeRecord saved = incomes.save(new IncomeRecord(current.id(), owner, current.profileId(),
                command.amount(), command.source(), current.active(), current.effectiveFrom()));
        return new ProfileModels.IncomeView(saved.id(), saved.amount(), saved.source());
    }
}
