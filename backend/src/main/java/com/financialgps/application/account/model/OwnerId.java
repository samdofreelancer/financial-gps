package com.financialgps.application.account.model;

import java.util.UUID;

/**
 * Identity &amp; Access value type (data-model.md, constitution §XIV "Identity &amp; Access"): an
 * immutable UUID wrapper for the authenticated owner.
 *
 * <p>Owned by the Identity &amp; Access context. It is produced ONLY by the platform
 * {@code CurrentOwnerProvider} adapter (resolved from the session) and is then passed explicitly
 * into input ports, so nothing below the API layer ever sees a session or principal. Other bounded
 * contexts may accept it as an <em>external actor identifier</em>, but must never derive one.
 */
public record OwnerId(UUID value) {
}
