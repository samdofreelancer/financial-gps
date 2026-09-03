package com.financialgps.platform.security;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Principal stored in the security context after register/login. Sessions stop here: everything
 * below the API layer receives only the {@code OwnerId} value resolved by
 * {@link CurrentOwnerProvider} (plan §Layer boundaries rule 2).
 *
 * <p>Implements {@link AuthenticatedPrincipal} so the session store persists the short owner id as
 * {@code SPRING_SESSION.principal_name} instead of the (long) record toString, and
 * {@link Serializable} because the security context is serialized into the Spring Session JDBC
 * attributes table.
 */
public record OwnerPrincipal(UUID id, String email, String role)
        implements AuthenticatedPrincipal, Serializable {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getName() {
        return id.toString();
    }
}
