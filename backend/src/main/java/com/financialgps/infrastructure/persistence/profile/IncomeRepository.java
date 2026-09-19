package com.financialgps.infrastructure.persistence.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped income access (fixture convention): no method can leak another owner's rows. */
public interface IncomeRepository extends JpaRepository<IncomeEntity, UUID> {

    List<IncomeEntity> findByOwnerIdOrderByCreatedAt(UUID ownerId);

    List<IncomeEntity> findByProfileIdAndOwnerId(UUID profileId, UUID ownerId);

    Optional<IncomeEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("delete from IncomeEntity r where r.id = :id and r.ownerId = :ownerId")
    int deleteByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
