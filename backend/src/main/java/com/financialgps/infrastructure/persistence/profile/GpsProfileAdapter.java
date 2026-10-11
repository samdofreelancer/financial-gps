package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;
import com.financialgps.application.profile.usecase.ProfileAssembler;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * GPS adapter for reading profile data.
 * Delegates to the existing ProfileAssembler for consistent assembly.
 */
@Component
public class GpsProfileAdapter implements com.financialgps.application.gps.port.out.GpsProfileReader {

    private final ProfileStore profiles;
    private final IncomeStore incomes;
    private final ExpenseStore expenses;
    private final com.financialgps.application.profile.port.out.ActiveDebts activeDebts;
    private final com.financialgps.application.profile.port.out.BusinessDate businessDate;

    public GpsProfileAdapter(ProfileStore profiles, IncomeStore incomes, ExpenseStore expenses,
                             com.financialgps.application.profile.port.out.ActiveDebts activeDebts,
                             com.financialgps.application.profile.port.out.BusinessDate businessDate) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
        this.activeDebts = activeDebts;
        this.businessDate = businessDate;
    }

    @Override
    public ProfileModels.ProfileView getProfile(OwnerId owner) {
        Optional<ProfileRecord> profileOpt = profiles.findByOwner(owner);
        if (profileOpt.isEmpty()) {
            return emptyProfile();
        }
        ProfileRecord profile = profileOpt.get();
        List<IncomeRecord> incomeRows = incomes.findAllByProfile(profile.id(), owner);
        List<ExpenseRecord> expenseRows = expenses.findAllByProfile(profile.id(), owner);
        List<com.financialgps.domain.debt.Debt> debts = activeDebts.findAllActive(owner, ProfileAssembler.currencyOf(profile));
        return ProfileAssembler.assemble(profile, incomeRows, expenseRows, debts, businessDate.today());
    }

    private ProfileModels.ProfileView emptyProfile() {
        return new ProfileModels.ProfileView(
                null, null, null, 0,
                List.of(), List.of(),
                null, null, null, null, null,
                List.of(), null
        );
    }
}