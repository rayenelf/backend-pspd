package com.pspd.backend.auth.repository;

import com.pspd.backend.auth.domain.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    
    Optional<PendingRegistration> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Modifying
    @Query("DELETE FROM PendingRegistration pr WHERE pr.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}