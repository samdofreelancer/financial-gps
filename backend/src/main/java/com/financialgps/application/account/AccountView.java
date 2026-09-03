package com.financialgps.application.account;

import java.time.Instant;
import java.util.UUID;

/** Account data shown to its owner (no credential material, ever). */
public record AccountView(UUID id, String email, Instant createdAt) {
}
