package com.financialgps.infrastructure.configuration;

import com.financialgps.application.account.model.PasswordPolicy;
import com.financialgps.application.account.model.PasswordRules;
import com.financialgps.application.account.port.in.AuthenticateOwner;
import com.financialgps.application.account.port.in.DeleteOwner;
import com.financialgps.application.account.port.in.ExportOwnerData;
import com.financialgps.application.account.port.in.GetAccount;
import com.financialgps.application.account.port.in.RegisterOwner;
import com.financialgps.application.account.port.out.AccountStore;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.application.account.port.out.PasswordHashing;
import com.financialgps.application.account.usecase.AuthenticateOwnerUseCase;
import com.financialgps.application.account.usecase.DeleteOwnerUseCase;
import com.financialgps.application.account.usecase.ExportOwnerDataUseCase;
import com.financialgps.application.account.usecase.GetAccountUseCase;
import com.financialgps.application.account.usecase.RegisterOwnerUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Explicit Identity &amp; Access wiring (plan Phase 2 step 4). Use cases are plain objects here; the
 * framework enters only through {@link UseCaseTransactions}, which wraps them at the input-port
 * boundary.
 *
 * <p>Transactions: {@code register} and {@code delete} are read-write; {@code authenticate} and
 * {@code me} are read-only. The export bundle reads only and was never transactional, so it stays
 * unwrapped — the observable behaviour is unchanged.
 */
@Configuration
class AccountUseCaseConfiguration {

    @Bean
    RegisterOwner registerOwner(AccountStore accounts, PasswordRules rules,
                               PasswordHashing passwordHashing, UseCaseTransactions transactions) {
        return transactions.writable(new RegisterOwnerUseCase(
                accounts, new PasswordPolicy(rules), passwordHashing));
    }

    @Bean
    AuthenticateOwner authenticateOwner(AccountStore accounts, PasswordHashing passwordHashing,
                                       UseCaseTransactions transactions) {
        return transactions.readOnly(new AuthenticateOwnerUseCase(accounts, passwordHashing));
    }

    @Bean
    GetAccount getAccount(AccountStore accounts, UseCaseTransactions transactions) {
        return transactions.readOnly(new GetAccountUseCase(accounts));
    }

    @Bean
    DeleteOwner deleteOwner(AccountStore accounts, UseCaseTransactions transactions) {
        return transactions.writable(new DeleteOwnerUseCase(accounts));
    }

    /** Every registered {@link OwnerDataSection} adapter contributes one bundle section. */
    @Bean
    ExportOwnerData exportOwnerData(AccountStore accounts, List<OwnerDataSection> sections) {
        return new ExportOwnerDataUseCase(accounts, sections);
    }
}
