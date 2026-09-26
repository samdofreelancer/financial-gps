package com.financialgps.application.profile.port.out;

import com.financialgps.application.account.model.OwnerId;

import java.util.UUID;

/**
 * Persistence-shaped profile facts exchanged with a {@link ProfileStore} adapter. Scalar facts only:
 * derived totals are recomputed by the engine, never stored (constitution §I).
 *
 * <p>{@code id} is {@code null} for a create and the stored row id for an update.
 */
public record ProfileRecord(UUID id, OwnerId owner, String currency, String savingsAmount,
                            String emergencyFundAmount, int dependentsCount) {
}
