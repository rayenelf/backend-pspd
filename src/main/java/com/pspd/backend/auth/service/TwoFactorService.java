package com.pspd.backend.auth.service;

import com.pspd.backend.auth.domain.OtpCode;
import com.pspd.backend.auth.repository.OtpCodeRepository;
import com.pspd.backend.common.mail.EmailService;
import com.pspd.backend.common.mail.EmailTemplates;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorService {

    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS    = 3;

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository    userRepository;
    private final EmailService      emailService;

    /** Génère un OTP 6 chiffres, le stocke haché (SHA-256) et l'envoie par email. */
    @Transactional
    public void generateAndSendOtp(User user) {
        otpCodeRepository.deleteByUserId(user.getId());

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        OtpCode otp = OtpCode.builder()
                .userId(user.getId())
                .codeHash(hashOtp(code))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .attempts(0)
                .used(false)
                .build();
        otpCodeRepository.save(otp);

        // Envoi du code uniquement par email (jamais loggué — donnée sensible).
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        emailService.send(user.getEmail(), "Votre code de connexion Domivo",
                EmailTemplates.otp(prenom, code, OTP_TTL_MINUTES));
        log.info("[2FA] Code OTP envoyé par email à {}", user.getEmail());
    }

    public enum VerifyStatus {
        SUCCESS,
        INVALID_CODE,
        EXPIRED,
        TOO_MANY_ATTEMPTS,
        USER_NOT_FOUND,
        NO_PENDING_OTP
    }

    public record VerifyResult(VerifyStatus status, User user) {}

    /** Vérifie le code fourni. Retourne SUCCESS + User si valide, sinon un statut d'erreur. */
    @Transactional
    public VerifyResult verify(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return new VerifyResult(VerifyStatus.USER_NOT_FOUND, null);

        User user = userOpt.get();
        Optional<OtpCode> otpOpt = otpCodeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        if (otpOpt.isEmpty()) return new VerifyResult(VerifyStatus.NO_PENDING_OTP, null);

        OtpCode otp = otpOpt.get();

        if (otp.isUsed())                                      return new VerifyResult(VerifyStatus.NO_PENDING_OTP, null);
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) return new VerifyResult(VerifyStatus.EXPIRED, null);
        if (otp.getAttempts() >= MAX_ATTEMPTS)                 return new VerifyResult(VerifyStatus.TOO_MANY_ATTEMPTS, null);

        if (!hashOtp(code).equals(otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpCodeRepository.save(otp);
            return new VerifyResult(VerifyStatus.INVALID_CODE, null);
        }

        otp.setUsed(true);
        otpCodeRepository.save(otp);
        return new VerifyResult(VerifyStatus.SUCCESS, user);
    }

    private String hashOtp(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}
