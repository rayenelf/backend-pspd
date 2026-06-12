package com.pspd.backend.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_codes")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "otp_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpType otpType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void init() {
        if (id == null)        id        = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum OtpType {
        TWO_FACTOR_AUTH,    // Pour la 2FA lors du login
        EMAIL_VERIFICATION  // Pour la vérification d'email lors de l'inscription
    }
}
