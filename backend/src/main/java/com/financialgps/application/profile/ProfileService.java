package com.financialgps.application.profile;

import com.financialgps.application.account.OwnerId;
import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.domain.engine.Assumptions;
import com.financialgps.domain.engine.FinancialEngine;
import com.financialgps.domain.engine.FinancialResult;
import com.financialgps.domain.engine.Provenance;
import com.financialgps.domain.model.Expense;
import com.financialgps.domain.model.FinancialInput;
import com.financialgps.domain.model.Income;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.policy.FinancialPolicy;
import com.financialgps.infrastructure.persistence.profile.ExpenseEntity;
import com.financialgps.infrastructure.persistence.profile.ExpenseRepository;
import com.financialgps.infrastructure.persistence.profile.IncomeEntity;
import com.financialgps.infrastructure.persistence.profile.IncomeRepository;
import com.financialgps.infrastructure.persistence.profile.ProfileEntity;
import com.financialgps.infrastructure.persistence.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owner-scoped profile use cases. Orchestration only — no formulas here. */
@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;

    public ProfileService(ProfileRepository profiles, IncomeRepository incomes, ExpenseRepository expenses) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
    }

    @Transactional(readOnly = true)
    public ProfileModels.ProfileView getProfile(OwnerId owner, LocalDate asOf) {
        ProfileEntity profile = profiles.findByOwnerId(owner.value()).orElse(null);
        List<IncomeEntity> incomeRows = profile == null ? List.of()
                : incomes.findByProfileIdAndOwnerId(profile.getId(), owner.value());
        List<ExpenseEntity> expenseRows = profile == null ? List.of()
                : expenses.findByProfileIdAndOwnerId(profile.getId(), owner.value());
        return assemble(profile, incomeRows, expenseRows, asOfOrToday(asOf));
    }

    @Transactional
    public ProfileModels.ProfileView putProfile(OwnerId owner, ProfileModels.PutProfileCommand cmd, LocalDate asOf) {
        ProfileEntity profile = profiles.findByOwnerId(owner.value()).orElse(null);
        if (profile == null) {
            profile = new ProfileEntity(owner.value(), cmd.currency(), cmd.savingsAmount(),
                    cmd.emergencyFundAmount(), cmd.dependentsCount());
            profiles.save(profile);
        } else {
            profile.setCurrency(cmd.currency());
            profile.setSavingsAmount(cmd.savingsAmount());
            profile.setEmergencyFundAmount(cmd.emergencyFundAmount());
            profile.setDependentsCount(cmd.dependentsCount());
            profile.touch();
            profiles.save(profile);
        }
        return assemble(profile,
                incomes.findByProfileIdAndOwnerId(profile.getId(), owner.value()),
                expenses.findByProfileIdAndOwnerId(profile.getId(), owner.value()),
                asOfOrToday(asOf));
    }

    @Transactional
    public ProfileModels.IncomeView addIncome(OwnerId owner, ProfileModels.IncomeCommand cmd) {
        ProfileEntity profile = requireProfile(owner);
        IncomeEntity saved = incomes.save(new IncomeEntity(owner.value(), profile.getId(),
                cmd.amount(), cmd.source(), true, cmd.effectiveFrom()));
        return new ProfileModels.IncomeView(saved.getId(), saved.getAmount().toPlainString(), saved.getSource());
    }

    @Transactional
    public ProfileModels.IncomeView updateIncome(OwnerId owner, UUID id, ProfileModels.IncomeCommand cmd) {
        IncomeEntity entity = incomes.findByIdAndOwnerId(id, owner.value())
                .orElseThrow(ResourceNotFoundException::new);
        entity.setAmount(cmd.amount());
        entity.setSource(cmd.source());
        entity.touch();
        IncomeEntity saved = incomes.save(entity);
        return new ProfileModels.IncomeView(saved.getId(), saved.getAmount().toPlainString(), saved.getSource());
    }

    @Transactional
    public void deleteIncome(OwnerId owner, UUID id) {
        if (incomes.deleteByIdAndOwnerId(id, owner.value()) == 0) {
            throw new ResourceNotFoundException();
        }
    }

    @Transactional
    public ProfileModels.ExpenseView addExpense(OwnerId owner, ProfileModels.ExpenseCommand cmd) {
        ProfileEntity profile = requireProfile(owner);
        ExpenseEntity saved = expenses.save(new ExpenseEntity(owner.value(), profile.getId(),
                cmd.amount(), cmd.category(), cmd.expenseType(), true, cmd.effectiveFrom()));
        return new ProfileModels.ExpenseView(saved.getId(), saved.getAmount().toPlainString(),
                saved.getCategory(), saved.getExpenseType());
    }

    @Transactional
    public ProfileModels.ExpenseView updateExpense(OwnerId owner, UUID id, ProfileModels.ExpenseCommand cmd) {
        ExpenseEntity entity = expenses.findByIdAndOwnerId(id, owner.value())
                .orElseThrow(ResourceNotFoundException::new);
        entity.setAmount(cmd.amount());
        entity.setCategory(cmd.category());
        entity.setExpenseType(cmd.expenseType());
        entity.touch();
        ExpenseEntity saved = expenses.save(entity);
        return new ProfileModels.ExpenseView(saved.getId(), saved.getAmount().toPlainString(),
                saved.getCategory(), saved.getExpenseType());
    }

    @Transactional
    public void deleteExpense(OwnerId owner, UUID id) {
        if (expenses.deleteByIdAndOwnerId(id, owner.value()) == 0) {
            throw new ResourceNotFoundException();
        }
    }

    private ProfileEntity requireProfile(OwnerId owner) {
        return profiles.findByOwnerId(owner.value()).orElseThrow(ResourceNotFoundException::new);
    }

    private ProfileModels.ProfileView assemble(ProfileEntity profile, List<IncomeEntity> incomeRows,
                                               List<ExpenseEntity> expenseRows, LocalDate asOf) {
        String currency = profile == null ? "VND" : profile.getCurrency();
        List<Income> domainIncomes = new ArrayList<>();
        for (IncomeEntity row : incomeRows) {
            domainIncomes.add(new Income(Money.of(row.getAmount().toPlainString(), currency),
                    row.getSource(), row.isActive(), row.getEffectiveFrom()));
        }
        List<Expense> domainExpenses = new ArrayList<>();
        for (ExpenseEntity row : expenseRows) {
            domainExpenses.add(new Expense(Money.of(row.getAmount().toPlainString(), currency),
                    row.getCategory(), Expense.ExpenseType.valueOf(row.getExpenseType()),
                    row.isActive(), row.getEffectiveFrom()));
        }
        FinancialResult result = FinancialEngine.calculate(
                new FinancialInput(domainIncomes, domainExpenses, List.of(), List.of()),
                Assumptions.none(), asOf, FinancialPolicy.defaults());
        List<Map<String, String>> incomeViews = new ArrayList<>();
        for (IncomeEntity row : incomeRows) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("id", row.getId().toString());
            m.put("amount", row.getAmount().toPlainString());
            m.put("currency", currency);
            m.put("source", row.getSource());
            m.put("provenance", "actual");
            incomeViews.add(m);
        }
        List<Map<String, String>> expenseViews = new ArrayList<>();
        for (ExpenseEntity row : expenseRows) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("id", row.getId().toString());
            m.put("amount", row.getAmount().toPlainString());
            m.put("currency", currency);
            m.put("category", row.getCategory());
            m.put("expenseType", row.getExpenseType());
            m.put("provenance", "actual");
            expenseViews.add(m);
        }
        return new ProfileModels.ProfileView(
                currency,
                profile == null ? "0.00" : profile.getSavingsAmount().toPlainString(),
                profile == null ? "0.00" : profile.getEmergencyFundAmount().toPlainString(),
                profile == null ? 0 : profile.getDependentsCount(),
                incomeViews, expenseViews,
                moneyView(result.position().income()), moneyView(result.position().expense()),
                moneyView(result.position().netCashFlow()), moneyView(result.position().availableCapacity()),
                provenanceViews(result), asOf.toString());
    }

    /** Provenance is carried through from the engine: every total says how it was derived. */
    private List<ProfileModels.ProvenanceView> provenanceViews(FinancialResult result) {
        List<ProfileModels.ProvenanceView> views = new ArrayList<>();
        for (Provenance entry : result.provenance()) {
            views.add(new ProfileModels.ProvenanceView(entry.field(), entry.kind(), entry.detail()));
        }
        return views;
    }

    private Map<String, String> moneyView(Money money) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("amount", money.asDecimalString());
        m.put("currency", money.currency());
        m.put("provenance", "calculated");
        return m;
    }

    private LocalDate asOfOrToday(LocalDate asOf) {
        return asOf == null ? LocalDate.now() : asOf;
    }
}
