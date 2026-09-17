package com.financialgps.infrastructure.persistence.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Owner-scoped profile access: every query takes ownerId explicitly. */
public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {

    Optional<ProfileEntity> findByOwnerId(UUID ownerId);

    Optional<ProfileEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("delete from ProfileEntity p where p.id = :id and p.ownerId = :ownerId")
    int deleteByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
