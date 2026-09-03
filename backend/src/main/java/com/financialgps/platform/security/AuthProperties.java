package com.financialgps.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuration surface for the authentication platform capability (plan §Configuration &amp;
 * secrets). Every key lives under {@code financial.auth.*} and is overridable by environment.
 *
 * @param sessionIdleTimeout FR-009 idle expiry (default 30m); also feeds {@code spring.session.timeout}
 * @param bcryptStrength     BCrypt cost factor (default 12)
 * @param cookie             session-cookie attributes (secure / same-site)
 * @param password           FR-005 password policy knobs
 */
@ConfigurationProperties(prefix = "financial.auth")
public record AuthProperties(
        @DefaultValue("30m") Duration sessionIdleTimeout,
        @DefaultValue("12") int bcryptStrength,
        @DefaultValue Cookie cookie,
        @DefaultValue Password password) {

    public record Cookie(@DefaultValue("true") boolean secure,
                         @DefaultValue("lax") String sameSite) {
    }

    public record Password(@DefaultValue("10") int minLength,
                           @DefaultValue("true") boolean requireLetter,
                           @DefaultValue("true") boolean requireDigit,
                           @DefaultValue("128") int maxLength) {
    }
}
