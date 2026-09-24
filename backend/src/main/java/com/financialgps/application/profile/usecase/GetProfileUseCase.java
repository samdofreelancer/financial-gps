package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.GetProfile;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;

import java.util.List;

/**
 * Read the owner's position: stored facts + server-calculated totals.
 *
 * <p>An owner without a profile is not an error — the position is reported with default facts and
 * zero totals (001 contract). The evaluation date comes from the {@link BusinessDate} port, never
 * from the HTTP adapter or a global clock.
 */
public final class GetProfileUseCase implements GetProfile {

    private final ProfileStore profiles;
    private final IncomeStore incomes;
    private final ExpenseStore expenses;
    private final BusinessDate businessDate;

    public GetProfileUseCase(ProfileStore profiles, IncomeStore incomes, ExpenseStore expenses,
                             BusinessDate businessDate) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
        this.businessDate = businessDate;
    }

    @Override
    public ProfileModels.ProfileView get(OwnerId owner) {
        ProfileRecord profile = profiles.findByOwner(owner).orElse(null);
        List<IncomeRecord> incomeRows = profile == null
                ? List.of() : incomes.findAllByProfile(profile.id(), owner);
        List<ExpenseRecord> expenseRows = profile == null
                ? List.of() : expenses.findAllByProfile(profile.id(), owner);
        return ProfileAssembler.assemble(profile, incomeRows, expenseRows, businessDate.today());
    }
}
