package com.financialgps.application.profile.port.out;

import com.financialgps.application.account.model.OwnerId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Persistence-shaped expense line exchanged with an {@link ExpenseStore} adapter.
 * {@code id} is {@code null} for a create and the stored row id for an update.
 */
public record ExpenseRecord(UUID id, OwnerId owner, UUID profileId, String amount, String category,
                            String expenseType, boolean active, LocalDate effectiveFrom) {
}
