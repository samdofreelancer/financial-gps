package com.financialgps.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** All email lookups go through {@code lower(email)} (research §5, FR-001/FR-004). */
public interface AccountRepository extends JpaRepository<AccountEntity, java.util.UUID> {

    @Query("select a from AccountEntity a where lower(a.email) = lower(:email)")
    Optional<AccountEntity> findByLowerEmail(@Param("email") String email);

    @Query("select count(a) > 0 from AccountEntity a where lower(a.email) = lower(:email)")
    boolean existsByLowerEmail(@Param("email") String email);
}
