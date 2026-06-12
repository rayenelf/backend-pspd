package com.pspd.backend.auth.repository;

import com.pspd.backend.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteByUserId(String userId);
}
