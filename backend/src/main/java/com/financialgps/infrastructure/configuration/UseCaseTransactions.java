package com.financialgps.infrastructure.configuration;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Demarcates the transaction at the INPUT PORT boundary instead of inside the use case (plan Phase 2
 * step 4).
 *
 * <p>Why a decorator rather than {@code @Transactional}: a use-case class must stay framework-free
 * (no Spring import, no annotation, no proxy assumption), yet "one request = one transaction" must
 * remain a property of the application boundary. The returned bean is a JDK proxy over the port
 * interface the use case implements, so collaborators still see nothing but the port. Rolling back on
 * {@code RuntimeException}/{@code Error} is the framework default and is deliberately not overridden
 * — the refactor preserves the previous read-only vs read-write behaviour exactly.
 */
@Component
class UseCaseTransactions {

    private final PlatformTransactionManager transactionManager;

    UseCaseTransactions(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /** Read-only boundary: matches the previous {@code @Transactional(readOnly = true)} use cases. */
    <T> T readOnly(T useCase) {
        return demarcate(useCase, true);
    }

    /** Read-write boundary: matches the previous {@code @Transactional} use cases. */
    <T> T writable(T useCase) {
        return demarcate(useCase, false);
    }

    @SuppressWarnings("unchecked")
    private <T> T demarcate(T useCase, boolean readOnly) {
        RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
        attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        attribute.setReadOnly(readOnly);

        NameMatchTransactionAttributeSource attributes = new NameMatchTransactionAttributeSource();
        attributes.addTransactionalMethod("*", attribute);

        ProxyFactory factory = new ProxyFactory(useCase);
        factory.addAdvice(new TransactionInterceptor(transactionManager, attributes));
        return (T) factory.getProxy();
    }
}
