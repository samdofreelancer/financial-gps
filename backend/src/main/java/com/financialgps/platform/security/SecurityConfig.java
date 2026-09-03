package com.financialgps.platform.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Single place for the route rules, CSRF handshake and session-cookie attributes
 * (plan §Architecture, area A1).
 *
 * <p>Route rules: everything is protected unless listed below (FR-006, SC-005). Public routes are
 * exactly the three authentication handshakes; even {@code /auth/logout} requires a session.
 *
 * <p>CSRF: double-submit cookie pattern — the {@code XSRF-TOKEN} cookie (readable by the SPA) is
 * echoed back as the {@code X-XSRF-TOKEN} header on state-changing requests (research §4). Spring
 * Security 6.2's {@code CsrfConfigurer} cannot attach a ProblemDetail access-denied handler, so
 * 007 owns the {@link CsrfFilter} instance (eager token materialization seeds the cookie on every
 * response, including {@code GET /auth/csrf}) and disables the built-in one.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    public CsrfFilter csrfFilter() {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null); // eager: cookie on every response
        CsrfFilter csrfFilter = new CsrfFilter(CookieCsrfTokenRepository.withHttpOnlyFalse());
        csrfFilter.setRequestHandler(requestHandler);
        csrfFilter.setAccessDeniedHandler(new ProblemDetailAccessDeniedHandler());
        return csrfFilter;
    }

    /** Prevent Boot from ALSO registering the CsrfFilter bean at the container level. */
    @Bean
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration(CsrfFilter csrfFilter) {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>(csrfFilter);
        registration.setEnabled(false); // it runs inside the security filter chain instead
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfFilter csrfFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // replaced by our own CsrfFilter instance above
                .addFilterBefore(csrfFilter, CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/csrf")
                        .permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                        .accessDeniedHandler(new ProblemDetailAccessDeniedHandler()));
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
    }

    /** Session-fixation defense: rotate the session id at sign-in instead of reusing it. */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    /** SESSION cookie: HttpOnly, SameSite from config, Secure from config (plan §Configuration). */
    @Bean
    public CookieSerializer cookieSerializer(AuthProperties properties) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(properties.cookie().secure());
        serializer.setSameSite(canonicalSameSite(properties.cookie().sameSite()));
        return serializer;
    }

    /** Render SameSite in its canonical capitalization (lax → Lax, strict → Strict, none → None). */
    private static String canonicalSameSite(String sameSite) {
        return switch (sameSite == null ? "" : sameSite.toLowerCase()) {
            case "lax" -> "Lax";
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> sameSite;
        };
    }
}
