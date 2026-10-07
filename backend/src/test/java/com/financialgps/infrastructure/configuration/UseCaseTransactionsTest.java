package com.financialgps.infrastructure.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plan Phase 2 step 4 (and the replacement for the old reflection-based {@code @Transactional}
 * assertion): the transaction boundary moved out of the use-case classes and onto the input-port
 * boundary, so "one request = one transaction" must be proven HERE, at the decorator.
 *
 * <p>The read-only vs read-write distinction and the rollback-on-{@code RuntimeException} default
 * are exactly the behaviour the previous annotations provided.
 */
class UseCaseTransactionsTest {

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final UseCaseTransactions transactions = new UseCaseTransactions(transactionManager);

    /** Minimal port + use case: the decorator must be testable without any real port. */
    interface SamplePort {
        String run();
    }

    private static final class SampleUseCase implements SamplePort {
        private final boolean fail;

        SampleUseCase(boolean fail) {
            this.fail = fail;
        }

        @Override
        public String run() {
            if (fail) {
                throw new IllegalStateException("business failure");
            }
            return "ok";
        }
    }

    private void givenAnActiveTransaction() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void theDecoratorExposesThePortInterfaceNotTheUseCaseClass() {
        Object decorated = transactions.writable(new SampleUseCase(false));

        assertThat(decorated).isInstanceOf(SamplePort.class);
        assertThat(decorated.getClass()).isNotEqualTo(SampleUseCase.class);
        assertThat(Proxy.isProxyClass(decorated.getClass()))
                .as("a JDK proxy over the input port keeps the use case framework-free")
                .isTrue();
    }

    @Test
    void aWritableUseCaseRunsInAReadWriteTransactionAndCommits() {
        givenAnActiveTransaction();

        String result = ((SamplePort) transactions.writable(new SampleUseCase(false))).run();

        assertThat(result).isEqualTo("ok");
        verify(transactionManager).getTransaction(argThat(definition -> !definition.isReadOnly()));
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void aReadOnlyUseCaseRunsInAReadOnlyTransaction() {
        givenAnActiveTransaction();

        ((SamplePort) transactions.readOnly(new SampleUseCase(false))).run();

        verify(transactionManager).getTransaction(argThat(TransactionDefinition::isReadOnly));
        verify(transactionManager).commit(any());
    }

    @Test
    void aRuntimeFailureRollsBackAndPropagates() {
        givenAnActiveTransaction();
        SamplePort decorated = (SamplePort) transactions.writable(new SampleUseCase(true));

        assertThatThrownBy(decorated::run)
                .as("the documented conflict/not-found contract must reach the API advice")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business failure");
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void aReadOnlyRuntimeFailureAlsoRollsBack() {
        givenAnActiveTransaction();
        SamplePort decorated = (SamplePort) transactions.readOnly(new SampleUseCase(true));

        assertThatThrownBy(decorated::run).isInstanceOf(IllegalStateException.class);
        verify(transactionManager).rollback(any());
    }
}
