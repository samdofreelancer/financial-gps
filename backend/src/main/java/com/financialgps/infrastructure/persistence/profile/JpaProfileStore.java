package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * JPA adapter for {@link ProfileStore}. {@code ProfileEntity} and {@code ProfileRepository} are
 * private collaborators: only immutable {@link ProfileRecord} values cross the port (plan Phase 2
 * steps 1–2).
 */
@Component
class JpaProfileStore implements ProfileStore {

    private final ProfileRepository profiles;

    JpaProfileStore(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Optional<ProfileRecord> findByOwner(OwnerId owner) {
        return profiles.findByOwnerId(owner.value()).map(JpaProfileStore::toRecord);
    }

    @Override
    public ProfileRecord save(ProfileRecord profile) {
        ProfileEntity entity;
        if (profile.id() == null) {
            entity = new ProfileEntity(profile.owner().value(), profile.currency(),
                    amount(profile.savingsAmount()), amount(profile.emergencyFundAmount()),
                    profile.dependentsCount());
        } else {
            // Owner-scoped lookup: a profile id from another owner can never be updated.
            entity = profiles.findByIdAndOwnerId(profile.id(), profile.owner().value())
                    .orElseThrow(ResourceNotFoundException::new);
            entity.setCurrency(profile.currency());
            entity.setSavingsAmount(amount(profile.savingsAmount()));
            entity.setEmergencyFundAmount(amount(profile.emergencyFundAmount()));
            entity.setDependentsCount(profile.dependentsCount());
            entity.touch();
        }
        return toRecord(profiles.save(entity));
    }

    private static BigDecimal amount(String decimalString) {
        return new BigDecimal(decimalString);
    }

    private static ProfileRecord toRecord(ProfileEntity entity) {
        return new ProfileRecord(entity.getId(), new OwnerId(entity.getOwnerId()),
                entity.getCurrency(),
                entity.getSavingsAmount().toPlainString(),
                entity.getEmergencyFundAmount().toPlainString(),
                entity.getDependentsCount());
    }
}
