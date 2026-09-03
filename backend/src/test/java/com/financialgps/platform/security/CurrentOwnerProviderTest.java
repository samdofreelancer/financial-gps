package com.financialgps.platform.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T012 — owner identity resolution from the security context only. */
class CurrentOwnerProviderTest {

    private final CurrentOwnerProvider provider = new CurrentOwnerProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsOwnerIdForAuthenticatedOwnerPrincipal() {
        UUID id = UUID.randomUUID();
        OwnerPrincipal principal = new OwnerPrincipal(id, "user@example.com", "OWNER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));

        assertThat(provider.requireCurrentOwner().value()).isEqualTo(id);
    }

    @Test
    void emptyContextThrowsAuthRequired() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(provider::requireCurrentOwner).isInstanceOf(AuthRequiredException.class);
    }

    @Test
    void nonOwnerPrincipalThrowsAuthRequired() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, java.util.List.of()));
        assertThatThrownBy(provider::requireCurrentOwner).isInstanceOf(AuthRequiredException.class);
    }
}
