package com.financialgps.infrastructure.configuration;

import com.financialgps.application.account.model.PasswordRules;
import com.financialgps.application.account.port.out.PasswordHashing;
import com.financialgps.infrastructure.security.BCryptPasswordHasher;
import com.financialgps.platform.security.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the platform's {@code financial.auth.*} configuration surface to the two framework-free
 * account collaborators the use cases depend on. This is the only place where Spring configuration
 * becomes an application value (plan Phase 2 step 3).
 */
@Configuration
class AccountAdapterConfiguration {

    /** BCrypt cost factor and the FR-005 policy knobs come from {@code financial.auth.*}. */
    @Bean
    PasswordHashing passwordHashing(AuthProperties properties) {
        return new BCryptPasswordHasher(properties.bcryptStrength());
    }

    @Bean
    PasswordRules passwordRules(AuthProperties properties) {
        AuthProperties.Password password = properties.password();
        return new PasswordRules(password.minLength(), password.requireLetter(),
                password.requireDigit(), password.maxLength());
    }
}
