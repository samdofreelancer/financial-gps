package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.gps.port.out.GpsProfileReader;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileStore;
import com.financialgps.application.profile.usecase.ProfileAssembler;
import com.financialgps.domain.gps.GpsProfileSnapshot;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * GPS adapter for reading profile data.
 *
 * <p>Builds the framework-free snapshot straight from the stored rows (with their
 * effective dating) instead of round-tripping through the assembled view, so the
 * position engine evaluates exactly the lines effective on {@code asOfDate}.
 */
@Component
public class GpsProfileAdapter implements GpsProfileReader {

    private final ProfileStore profiles;
    private final IncomeStore incomes;
    private final ExpenseStore expenses;

    public GpsProfileAdapter(ProfileStore profiles, IncomeStore incomes, ExpenseStore expenses) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
    }

    @Override
    public GpsProfileSnapshot getProfile(OwnerId owner, LocalDate asOfDate) {
        var profileOpt = profiles.findByOwner(owner);
        if (profileOpt.isEmpty()) {
            return new GpsProfileSnapshot(null, List.of(), List.of(), null, null, 0);
        }
        var profile = profileOpt.get();
        List<GpsProfileSnapshot.IncomeLine> incomeLines =
                incomes.findAllByProfile(profile.id(), owner).stream()
                        .map(row -> new GpsProfileSnapshot.IncomeLine(
                                row.amount(), row.source(), row.active(), row.effectiveFrom()))
                        .toList();
        List<GpsProfileSnapshot.ExpenseLine> expenseLines =
                expenses.findAllByProfile(profile.id(), owner).stream()
                        .map(row -> new GpsProfileSnapshot.ExpenseLine(row.amount(), row.category(),
                                row.expenseType(), row.active(), row.effectiveFrom()))
                        .toList();
        return new GpsProfileSnapshot(
                ProfileAssembler.currencyOf(profile),
                incomeLines,
                expenseLines,
                profile.savingsAmount(),
                profile.emergencyFundAmount(),
                profile.dependentsCount());
    }
}