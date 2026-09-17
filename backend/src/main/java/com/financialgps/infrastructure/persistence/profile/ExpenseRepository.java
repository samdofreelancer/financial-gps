package com.financialgps.infrastructure.persistence.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped expense access (fixture convention). */
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {

    List<ExpenseEntity> findByOwnerIdOrderByCreatedAt(UUID ownerId);

    List<ExpenseEntity> findByProfileIdAndOwnerId(UUID profileId, UUID ownerId);

    Optional<ExpenseEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("delete from ExpenseEntity r where r.id = :id and r.ownerId = :ownerId")
    int deleteByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
