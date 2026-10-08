package com.financialgps.application.profile.usecase;

import com.financialgps.domain.model.Obligation;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.PutProfile;
import com.financialgps.application.profile.port.out.ActiveDebts;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;

import java.util.List;

/**
 * Create-or-replace the owner's profile (PUT semantics) and answer with the saved representation,
 * so the client never has to follow up with a read to learn the recalculated position.
 *
 * <p>The answer carries the same totals as {@code GET /profile}, debts included: a PUT response that
 * omitted mandatory payments would contradict the very next read.
 */
public final class PutProfileUseCase implements PutProfile {

    private final ProfileStore profiles;
    private final IncomeStore incomes;
    private final ExpenseStore expenses;
    private final ActiveDebts activeDebts;
    private final BusinessDate businessDate;

    public PutProfileUseCase(ProfileStore profiles, IncomeStore incomes, ExpenseStore expenses,
                             ActiveDebts activeDebts, BusinessDate businessDate) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
        this.activeDebts = activeDebts;
        this.businessDate = businessDate;
    }

    @Override
    public ProfileModels.ProfileView put(OwnerId owner, ProfileModels.PutProfileCommand command) {
        ProfileRecord existing = profiles.findByOwner(owner).orElse(null);
        ProfileRecord saved = profiles.save(new ProfileRecord(
                existing == null ? null : existing.id(),
                owner,
                command.currency(),
                command.savingsAmount(),
                command.emergencyFundAmount(),
                command.dependentsCount()));
        List<Obligation> obligations = activeDebts.findAllActive(owner,
                ProfileAssembler.currencyOf(saved));
        return ProfileAssembler.assemble(saved,
                incomes.findAllByProfile(saved.id(), owner),
                expenses.findAllByProfile(saved.id(), owner),
                obligations,
                businessDate.today());
    }
}
