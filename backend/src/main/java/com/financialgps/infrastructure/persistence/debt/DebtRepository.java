package com.financialgps.infrastructure.persistence.debt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped debt access: no method can leak another owner's rows. */
public interface DebtRepository extends JpaRepository<DebtEntity, UUID> {

    List<DebtEntity> findByOwnerIdOrderByCreatedAt(UUID ownerId);

    List<DebtEntity> findByOwnerIdAndStatusInOrderByCreatedAt(UUID ownerId, List<String> statuses);

    Optional<DebtEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying(clearAutomatically = true)
    @Query("update DebtEntity d set d.status = 'ARCHIVED', d.updatedAt = CURRENT_TIMESTAMP where d.id = :id and d.ownerId = :ownerId and d.status <> 'ARCHIVED'")
    int archiveByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
