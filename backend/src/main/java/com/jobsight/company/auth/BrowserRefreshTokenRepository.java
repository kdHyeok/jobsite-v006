package com.jobsight.company.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface BrowserRefreshTokenRepository extends JpaRepository<BrowserRefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from BrowserRefreshToken t where t.tokenHash = :hash")
    Optional<BrowserRefreshToken> findByTokenHashForUpdate(@Param("hash") String hash);

    void deleteByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(Instant now);
}
