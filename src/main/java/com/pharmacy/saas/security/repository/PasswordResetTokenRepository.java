package com.pharmacy.saas.security.repository;

import com.pharmacy.saas.security.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedFalse(UUID userId);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.userId = :userId AND prt.used = false AND prt.expiresAt > :now")
    List<PasswordResetToken> findValidTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    void deleteByUserId(UUID userId);
}