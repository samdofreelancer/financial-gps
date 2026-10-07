package com.financialgps.application.profile.usecase;

import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.GetProfile;
import com.financialgps.application.profile.port.out.ActiveDebts;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;
import com.financialgps.domain.debt.Debt;

import java.util.List;

/**
 * Read the owner's position: stored facts + server-calculated totals.
 *
 * <p>An owner without a profile is not an error — the position is reported with default facts and
 * zero totals (001 contract). The evaluation date comes from the {@link BusinessDate} port, never
 * from the HTTP adapter or a global clock.
 *
 * <p>002 closes the loop Feature 001 left open: the owner's ACTIVE debts are loaded through
 * {@link ActiveDebts} and flow into {@code Portfolio.debts()}, so their mandatory minimum
 * payments reduce Net Cash Flow and Available Capacity (spec §8.2, SC5.1).
 */
public final class GetProfileUseCase implements GetProfile {

    private final ProfileStore profiles;
    private final IncomeStore incomes;
    private final ExpenseStore expenses;
    private final ActiveDebts activeDebts;
    private final BusinessDate businessDate;

    public GetProfileUseCase(ProfileStore profiles, IncomeStore incomes, ExpenseStore expenses,
                             ActiveDebts activeDebts, BusinessDate businessDate) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
        this.activeDebts = activeDebts;
        this.businessDate = businessDate;
    }

    @Override
    public ProfileModels.ProfileView get(OwnerId owner) {
        ProfileRecord profile = profiles.findByOwner(owner).orElse(null);
        List<IncomeRecord> incomeRows = profile == null
                ? List.of() : incomes.findAllByProfile(profile.id(), owner);
        List<ExpenseRecord> expenseRows = profile == null
                ? List.of() : expenses.findAllByProfile(profile.id(), owner);
        List<Debt> debts = activeDebts.findAllActive(owner, ProfileAssembler.currencyOf(profile));
        return ProfileAssembler.assemble(profile, incomeRows, expenseRows, debts,
                businessDate.today());
    }
}
