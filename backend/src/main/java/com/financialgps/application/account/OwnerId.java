package com.financialgps.application.account;

import java.util.UUID;

/**
 * Application-layer value type (data-model.md): immutable UUID wrapper for the authenticated
 * owner. Produced only by {@code CurrentOwnerProvider}; passed explicitly into services and
 * owner-scoped repositories so nothing below the API layer ever sees a session or principal.
 */
public record OwnerId(UUID value) {
}
