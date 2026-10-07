package com.financialgps.application.account.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence-shaped account facts exchanged with an {@link AccountStore} adapter. Immutable: JPA
 * entities never leave the infrastructure lane (plan Phase 2 step 2). {@code id} is assigned by the
 * database.
 */
public record AccountRecord(UUID id, String email, String passwordHash, String role, Instant createdAt) {
}
