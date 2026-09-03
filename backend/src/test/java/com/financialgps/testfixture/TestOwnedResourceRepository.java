package com.financialgps.testfixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owner-scoped repository convention (FR-007/FR-008): every query takes {@code ownerId} as an
 * explicit parameter — no method can return another owner's rows.
 */
public interface TestOwnedResourceRepository extends JpaRepository<TestOwnedResourceEntity, UUID> {

    List<TestOwnedResourceEntity> findByOwnerIdOrderByLabel(UUID ownerId);

    Optional<TestOwnedResourceEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("delete from TestOwnedResourceEntity r where r.id = :id and r.ownerId = :ownerId")
    int deleteByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
