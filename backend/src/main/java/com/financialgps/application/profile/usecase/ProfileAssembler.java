package com.financialgps.application.profile.usecase;

import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.domain.engine.Assumptions;
import com.financialgps.domain.engine.FinancialEngine;
import com.financialgps.domain.engine.FinancialResult;
import com.financialgps.domain.engine.Provenance;
import com.financialgps.domain.model.Expense;
import com.financialgps.domain.model.FinancialInput;
import com.financialgps.domain.model.Income;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.policy.FinancialPolicy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps stored profile facts onto the canonical engine and back into a typed view.
 *
 * <p>This is the "external input → domain input" step of the application layer (constitution §XIV
 * "Application service"): it consumes PORT RECORDS only — never a JPA entity, repository or HTTP
 * DTO. No financial formula lives here: every total comes from {@link FinancialEngine}, and every
 * total states its provenance so stored facts stay distinguishable from calculated values
 * (constitution §III/§V).
 */
final class ProfileAssembler {

    /** Currency assumed when the owner has not recorded a profile yet. */
    private static final String DEFAULT_CURRENCY = "VND";

    private ProfileAssembler() {
    }

    static ProfileModels.ProfileView assemble(ProfileRecord profile,
                                             List<IncomeRecord> incomeRows,
                                             List<ExpenseRecord> expenseRows,
                                             LocalDate asOf) {
        String currency = profile == null ? DEFAULT_CURRENCY : profile.currency();
        FinancialResult result = FinancialEngine.calculate(
                new FinancialInput(domainIncomes(incomeRows, currency),
                        domainExpenses(expenseRows, currency), List.of(), List.of()),
                Assumptions.none(), asOf, FinancialPolicy.defaults());

        List<ProfileModels.IncomeLineView> incomeViews = new ArrayList<>();
        for (IncomeRecord row : incomeRows) {
            incomeViews.add(new ProfileModels.IncomeLineView(row.id().toString(), row.amount(),
                    currency, row.source(), "actual"));
        }
        List<ProfileModels.ExpenseLineView> expenseViews = new ArrayList<>();
        for (ExpenseRecord row : expenseRows) {
            expenseViews.add(new ProfileModels.ExpenseLineView(row.id().toString(), row.amount(),
                    currency, row.category(), row.expenseType(), "actual"));
        }

        return new ProfileModels.ProfileView(
                currency,
                profile == null ? "0.00" : profile.savingsAmount(),
                profile == null ? "0.00" : profile.emergencyFundAmount(),
                profile == null ? 0 : profile.dependentsCount(),
                List.copyOf(incomeViews), List.copyOf(expenseViews),
                moneyView(result.position().income(), "calculated"),
                moneyView(result.position().expense(), "calculated"),
                moneyView(result.position().netCashFlow(), "calculated"),
                moneyView(result.position().availableCapacity(), "calculated"),
                provenanceViews(result),
                asOf.toString());
    }

    private static List<Income> domainIncomes(List<IncomeRecord> rows, String currency) {
        List<Income> incomes = new ArrayList<>();
        for (IncomeRecord row : rows) {
            incomes.add(new Income(Money.of(row.amount(), currency), row.source(),
                    row.active(), row.effectiveFrom()));
        }
        return incomes;
    }

    private static List<Expense> domainExpenses(List<ExpenseRecord> rows, String currency) {
        List<Expense> expenses = new ArrayList<>();
        for (ExpenseRecord row : rows) {
            expenses.add(new Expense(Money.of(row.amount(), currency), row.category(),
                    Expense.ExpenseType.valueOf(row.expenseType()), row.active(), row.effectiveFrom()));
        }
        return expenses;
    }

    /** Provenance is carried through from the engine: every total says how it was derived. */
    private static List<ProfileModels.ProvenanceView> provenanceViews(FinancialResult result) {
        List<ProfileModels.ProvenanceView> views = new ArrayList<>();
        for (Provenance entry : result.provenance()) {
            views.add(new ProfileModels.ProvenanceView(entry.field(), entry.kind(), entry.detail()));
        }
        return views;
    }

    private static ProfileModels.MoneyView moneyView(Money money, String provenance) {
        return new ProfileModels.MoneyView(money.asDecimalString(), money.currency(), provenance);
    }
}
