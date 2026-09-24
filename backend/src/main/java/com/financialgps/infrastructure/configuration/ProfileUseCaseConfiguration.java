package com.financialgps.infrastructure.configuration;

import com.financialgps.application.profile.port.in.AddExpense;
import com.financialgps.application.profile.port.in.AddIncome;
import com.financialgps.application.profile.port.in.DeleteExpense;
import com.financialgps.application.profile.port.in.DeleteIncome;
import com.financialgps.application.profile.port.in.GetProfile;
import com.financialgps.application.profile.port.in.PutProfile;
import com.financialgps.application.profile.port.in.UpdateExpense;
import com.financialgps.application.profile.port.in.UpdateIncome;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileStore;
import com.financialgps.application.profile.usecase.AddExpenseUseCase;
import com.financialgps.application.profile.usecase.AddIncomeUseCase;
import com.financialgps.application.profile.usecase.DeleteExpenseUseCase;
import com.financialgps.application.profile.usecase.DeleteIncomeUseCase;
import com.financialgps.application.profile.usecase.GetProfileUseCase;
import com.financialgps.application.profile.usecase.PutProfileUseCase;
import com.financialgps.application.profile.usecase.UpdateExpenseUseCase;
import com.financialgps.application.profile.usecase.UpdateIncomeUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit Financial Profile wiring (plan Phase 2 step 4): one input port per use case, each
 * transaction-demarcated at the boundary. Read use cases are read-only, mutations are read-write.
 */
@Configuration
class ProfileUseCaseConfiguration {

    @Bean
    GetProfile getProfile(ProfileStore profiles, IncomeStore incomes, ExpenseStore expenses,
                          BusinessDate businessDate, UseCaseTransactions transactions) {
        return transactions.readOnly(
                new GetProfileUseCase(profiles, incomes, expenses, businessDate));
    }

    @Bean
    PutProfile putProfile(ProfileStore profiles, IncomeStore incomes, ExpenseStore expenses,
                          BusinessDate businessDate, UseCaseTransactions transactions) {
        return transactions.writable(
                new PutProfileUseCase(profiles, incomes, expenses, businessDate));
    }

    @Bean
    AddIncome addIncome(ProfileStore profiles, IncomeStore incomes, BusinessDate businessDate,
                        UseCaseTransactions transactions) {
        return transactions.writable(new AddIncomeUseCase(profiles, incomes, businessDate));
    }

    @Bean
    UpdateIncome updateIncome(IncomeStore incomes, UseCaseTransactions transactions) {
        return transactions.writable(new UpdateIncomeUseCase(incomes));
    }

    @Bean
    DeleteIncome deleteIncome(IncomeStore incomes, UseCaseTransactions transactions) {
        return transactions.writable(new DeleteIncomeUseCase(incomes));
    }

    @Bean
    AddExpense addExpense(ProfileStore profiles, ExpenseStore expenses, BusinessDate businessDate,
                          UseCaseTransactions transactions) {
        return transactions.writable(new AddExpenseUseCase(profiles, expenses, businessDate));
    }

    @Bean
    UpdateExpense updateExpense(ExpenseStore expenses, UseCaseTransactions transactions) {
        return transactions.writable(new UpdateExpenseUseCase(expenses));
    }

    @Bean
    DeleteExpense deleteExpense(ExpenseStore expenses, UseCaseTransactions transactions) {
        return transactions.writable(new DeleteExpenseUseCase(expenses));
    }
}
