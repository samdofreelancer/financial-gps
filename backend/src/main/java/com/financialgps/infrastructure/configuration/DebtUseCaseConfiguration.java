package com.financialgps.infrastructure.configuration;

import com.financialgps.application.debt.port.in.DeleteDebt;
import com.financialgps.application.debt.port.in.GetDebtSchedule;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.debt.port.in.GetDebts;
import com.financialgps.application.debt.port.in.MarkDebtPayment;
import com.financialgps.application.debt.port.in.RecordDebt;
import com.financialgps.application.debt.port.in.UpdateDebt;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.application.debt.usecase.DeleteDebtUseCase;
import com.financialgps.application.debt.usecase.GetDebtScheduleUseCase;
import com.financialgps.application.debt.usecase.GetDebtSummaryUseCase;
import com.financialgps.application.debt.usecase.GetDebtsUseCase;
import com.financialgps.application.debt.usecase.MarkDebtPaymentUseCase;
import com.financialgps.application.debt.usecase.RecordDebtUseCase;
import com.financialgps.application.debt.usecase.UpdateDebtUseCase;
import com.financialgps.domain.debt.DebtStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit debt wiring: one input port per use case, each transaction-demarcated at the
 * boundary. Read use cases are read-only, mutations are read-write.
 */
@Configuration
class DebtUseCaseConfiguration {

    @Bean
    RecordDebt recordDebt(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates,
                          UseCaseTransactions transactions) {
        return transactions.writable(new RecordDebtUseCase(debts, currency, dates));
    }

    @Bean
    UpdateDebt updateDebt(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates,
                         UseCaseTransactions transactions) {
        return transactions.writable(new UpdateDebtUseCase(debts, currency, dates));
    }

    @Bean
    DeleteDebt deleteDebt(DebtStore debts, UseCaseTransactions transactions) {
        return transactions.writable(new DeleteDebtUseCase(debts));
    }

    @Bean
    GetDebts getDebts(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates,
                      UseCaseTransactions transactions) {
        return transactions.readOnly(new GetDebtsUseCase(debts, currency, dates));
    }

    @Bean
    GetDebtSchedule getDebtSchedule(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates,
                                    UseCaseTransactions transactions) {
        return transactions.readOnly(new GetDebtScheduleUseCase(debts, currency, dates));
    }

    @Bean
    GetDebtSummary getDebtSummary(DebtStore debts, DebtCurrency currency, DebtIncomeReader incomes,
                                  DebtBusinessDate dates, UseCaseTransactions transactions) {
        return transactions.readOnly(new GetDebtSummaryUseCase(debts, currency, incomes, dates));
    }

    @Bean
    MarkDebtPayment markDebtPayment(DebtStore debts, DebtCurrency currency, DebtBusinessDate dates,
                                    UseCaseTransactions transactions) {
        return transactions.writable(new MarkDebtPaymentUseCase(debts, currency, dates));
    }
}
