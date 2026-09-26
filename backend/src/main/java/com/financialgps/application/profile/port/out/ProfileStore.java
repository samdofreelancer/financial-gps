package com.financialgps.application.profile.port.out;

import com.financialgps.application.account.model.OwnerId;

import java.util.Optional;

/**
 * Profile persistence port. One profile row per owner; every query is owner-scoped so no adapter can
 * leak another owner's row.
 */
public interface ProfileStore {

    Optional<ProfileRecord> findByOwner(OwnerId owner);

    /**
     * Create-or-replace the owner's profile row. {@code record.id()} of {@code null} means create;
     * otherwise the existing row for that id and owner is updated. The stored record (with its
     * assigned id) is returned.
     */
    ProfileRecord save(ProfileRecord profile);
}
