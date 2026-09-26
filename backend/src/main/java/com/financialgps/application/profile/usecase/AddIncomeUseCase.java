package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.AddIncome;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;

/**
 * Add an income line to the owner's profile. A line cannot exist without its profile, so a missing
 * profile is the same {@code 404 RESOURCE_NOT_FOUND} outcome as a missing line (FR-010).
 */
public final class AddIncomeUseCase implements AddIncome {

    private final ProfileStore profiles;
    private final IncomeStore incomes;
    private final BusinessDate businessDate;

    public AddIncomeUseCase(ProfileStore profiles, IncomeStore incomes, BusinessDate businessDate) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.businessDate = businessDate;
    }

    @Override
    public ProfileModels.IncomeView add(OwnerId owner, ProfileModels.IncomeCommand command) {
        ProfileRecord profile = profiles.findByOwner(owner).orElseThrow(ResourceNotFoundException::new);
        IncomeRecord saved = incomes.save(new IncomeRecord(null, owner, profile.id(),
                command.amount(), command.source(), true, businessDate.today()));
        return new ProfileModels.IncomeView(saved.id(), saved.amount(), saved.source());
    }
}
