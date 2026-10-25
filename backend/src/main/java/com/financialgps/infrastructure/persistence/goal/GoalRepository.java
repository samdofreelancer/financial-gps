package com.financialgps.infrastructure.persistence.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped goal access: no method can leak another owner's rows. */
public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {

    List<GoalEntity> findByOwnerIdAndStatusInOrderByPriorityAscCreatedAtAscIdAsc(
            UUID ownerId, List<String> statuses);

    Optional<GoalEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying(clearAutomatically = true)
    @Query("update GoalEntity g set g.status = 'ARCHIVED', g.updatedAt = CURRENT_TIMESTAMP where g.id = :id and g.ownerId = :ownerId and g.status <> 'ARCHIVED'")
    int archiveByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
