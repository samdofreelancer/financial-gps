package com.financialgps.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Signs an authenticated owner in / out over the servlet session (Spring Session JDBC backend).
 *
 * <p>Sign-in order mirrors the plan's session-fixation defense (plan §Security flow/Login
 * step 3): the session id is rotated ({@code changeSessionId}) BEFORE the security context is
 * saved, so a pre-authentication session id is never reused for an authenticated session.
 */
@Component
public class SessionAuthenticator {

    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final AuthProperties properties;

    public SessionAuthenticator(SecurityContextRepository securityContextRepository,
                                SessionAuthenticationStrategy sessionAuthenticationStrategy,
                                AuthProperties properties) {
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.properties = properties;
    }

    public void signIn(OwnerPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.authorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        securityContextRepository.saveContext(context, request, response);
    }

    /** Server-side session termination (FR-003) + cookie removal. Idempotent. */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
                .logout(request, response, authentication);
        SecurityContextHolder.clearContext();

        ResponseCookie cleared = ResponseCookie.from("SESSION", "")
                .path("/")
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    }
}
