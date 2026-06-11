package com.pspd.backend.auth.repository;

import com.pspd.backend.auth.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {
    Optional<OtpCode> findTopByUserIdOrderByCreatedAtDesc(String userId);
    void deleteByUserId(String userId);
}
