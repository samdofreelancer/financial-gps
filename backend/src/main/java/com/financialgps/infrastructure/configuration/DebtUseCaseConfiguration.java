package com.financialgps.infrastructure.configuration;

import com.financialgps.application.debt.port.in.DeleteDebt;
import com.financialgps.application.debt.port.in.GetDebtSchedule;
import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.debt.port.in.GetDebts;
import com.financialgps.application.debt.port.in.MarkDebtPayment;
import com.financialgps.application.debt.port.in.RecordDebt;
import com.financialgps.application.debt.port.in.UpdateDebt;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.application.debt.usecase.DebtUseCases;
import com.financialgps.domain.debt.DebtStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Explicit debt wiring: one input port per use case, transaction-demarcated at the boundary. */
@Configuration
class DebtUseCaseConfiguration {

    @Bean
    DebtUseCases debtUseCases(DebtStore debts, DebtIncomeReader incomes, DebtBusinessDate dates) {
        return new DebtUseCases(debts, incomes, dates);
    }

    @Bean
    RecordDebt recordDebt(DebtUseCases useCases, UseCaseTransactions transactions) {
        return transactions.writable(new RecordDebt() {
            @Override
            public com.financialgps.application.debt.model.DebtModels.DebtView record(
                    com.financialgps.domain.model.OwnerId owner,
                    com.financialgps.application.debt.model.DebtModels.DebtCommand command) {
                return useCases.record(owner, command);
            }
        });
    }

    @Bean
    UpdateDebt updateDebt(DebtUseCases useCases, UseCaseTransactions transactions) {
        return transactions.writable(new UpdateDebt() {
            @Override
            public com.financialgps.application.debt.model.DebtModels.DebtView update(
                    com.financialgps.domain.model.OwnerId owner, java.util.UUID id,
                    com.financialgps.application.debt.model.DebtModels.DebtUpdateCommand command) {
                return useCases.update(owner, id, command);
            }
        });
    }

    @Bean
    DeleteDebt deleteDebt(DebtUseCases useCases, UseCaseTransactions transactions) {
        return transactions.writable(new DeleteDebt() {
            @Override
            public void delete(com.financialgps.domain.model.OwnerId owner,
                               java.util.UUID id) {
                useCases.delete(owner, id);
            }
        });
    }

    @Bean
    GetDebts getDebts(DebtUseCases useCases, UseCaseTransactions transactions) {
        return transactions.readOnly(new GetDebts() {
            @Override
            public com.financialgps.application.debt.model.DebtModels.DebtView get(
                    com.financialgps.domain.model.OwnerId owner, java.util.UUID id) {
                return useCases.get(owner, id);
            }

            @Override
            public java.util.List<com.financialgps.application.debt.model.DebtModels.DebtView> list(
                    com.financialgps.domain.model.OwnerId owner) {
                return useCases.list(owner);
            }
        });
    }

    @Bean
    GetDebtSchedule getDebtSchedule(DebtUseCases useCases, UseCaseTransactions transactions) {
        return transactions.readOnly(new GetDebtSchedule() {
            @Override
            public com.financialgps.application.debt.model.DebtModels.DebtScheduleView schedule(
                    com.financialgps.domain.model.OwnerId owner, java.util.UUID id) {
                return useCases.schedule(owner, id);
            }
        });
    }

    @Bean
    GetDebtSummary getDebtSummary(DebtUseCases useCases, UseCaseTransactions transactions) {
        return transactions.readOnly(new GetDebtSummary() {
            @Override
            public com.financialgps.application.debt.model.DebtModels.DebtSummaryView summary(
                    com.financialgps.domain.model.OwnerId owner) {
                return useCases.summary(owner);
            }
        });
    }

    @Bean
    MarkDebtPayment markDebtPayment(DebtUseCases useCases, UseCaseTransactions transactions) {
        return transactions.writable(new MarkDebtPayment() {
            @Override
            public com.financialgps.application.debt.model.DebtModels.DebtView markPaid(
                    com.financialgps.domain.model.OwnerId owner, java.util.UUID id) {
                return useCases.markPaid(owner, id);
            }

            @Override
            public com.financialgps.application.debt.model.DebtModels.DebtView undoMark(
                    com.financialgps.domain.model.OwnerId owner, java.util.UUID id) {
                return useCases.undoMark(owner, id);
            }
        });
    }
}
