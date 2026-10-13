package com.financialgps.infrastructure.configuration;

import com.financialgps.application.debt.port.in.GetDebtSummary;
import com.financialgps.application.debt.port.out.DebtBusinessDate;
import com.financialgps.application.debt.port.out.DebtIncomeReader;
import com.financialgps.application.debt.port.out.DebtStore;
import com.financialgps.application.debt.usecase.DebtUseCases;
import com.financialgps.domain.model.OwnerId;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 004 review: the {@code GetDebtSummary} bean must honour an explicit asOfDate for the GPS debt
 * leg. The previous anonymous bean only overrode {@code summary(owner)}; the two-arg call fell to
 * the interface default, which discards the date and evaluates at the business date (today).
 * Proving it through the configured bean, not the use case, catches a future regression where the
 * wiring silently drops the date again (review item 1).
 */
class DebtUseCaseConfigurationTest {

    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());
    private static final LocalDate BUSINESS_TODAY = LocalDate.of(2026, 1, 1);
    private static final LocalDate REQUESTED_AS_OF = LocalDate.of(2026, 5, 5);

    private final DebtStore debts = mock(DebtStore.class);
    private final DebtIncomeReader incomes = mock(DebtIncomeReader.class);
    private final DebtBusinessDate dates = () -> BUSINESS_TODAY;
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    private GetDebtSummary configuredBean() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        DebtUseCases useCases = new DebtUseCases(debts, incomes, dates);
        return new DebtUseCaseConfiguration().getDebtSummary(useCases,
                new UseCaseTransactions(transactionManager));
    }

    @Test
    void twoArgSummary_routesToTheDateAwareVariant_notTheBusinessDate() {
        AtomicReference<LocalDate> incomeSeenAsOf = new AtomicReference<>();
        when(incomes.totalActiveMonthlyIncome(any(), any())).thenAnswer(inv -> {
            incomeSeenAsOf.set(inv.getArgument(1));
            return Optional.of(new BigDecimal("1000000.00"));
        });
        when(debts.findAllByOwner(any())).thenReturn(List.of());

        GetDebtSummary bean = configuredBean();
        var view = bean.summary(OWNER, REQUESTED_AS_OF);

        // The DTI denominator (and the whole summary) was evaluated as of the requested date.
        assertThat(incomeSeenAsOf).hasValue(REQUESTED_AS_OF);
        assertThat(view.asOf()).isEqualTo(REQUESTED_AS_OF.toString());
        verify(incomes, never()).totalActiveMonthlyIncome(any(), eq(BUSINESS_TODAY));
    }
}