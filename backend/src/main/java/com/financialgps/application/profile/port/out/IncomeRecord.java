package com.financialgps.application.profile.port.out;

import com.financialgps.application.account.model.OwnerId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Persistence-shaped income line exchanged with an {@link IncomeStore} adapter.
 * {@code id} is {@code null} for a create and the stored row id for an update.
 */
public record IncomeRecord(UUID id, OwnerId owner, UUID profileId, String amount, String source,
                           boolean active, LocalDate effectiveFrom) {
}
