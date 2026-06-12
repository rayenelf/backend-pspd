package com.pspd.backend.auth.service;

import com.pspd.backend.auth.domain.PasswordResetToken;
import com.pspd.backend.auth.repository.PasswordResetTokenRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.mail.EmailService;
import com.pspd.backend.common.mail.EmailTemplates;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Mot de passe oublié : génère un lien de réinitialisation (TTL 1 h, usage unique),
 * l'envoie par email, puis applique le nouveau mot de passe.
 * À la réinitialisation, toutes les sessions sont révoquées (sécurité).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TTL_MINUTES = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository               userRepository;
    private final PasswordEncoder              passwordEncoder;
    private final EmailService                 emailService;
    private final SessionService               sessionService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * Envoie un lien de réinitialisation si le compte existe.
     * Réponse identique dans tous les cas (pas de fuite sur l'existence d'un email).
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.deleteByUserId(user.getId());

            String rawToken = randomToken();
            tokenRepository.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(hash(rawToken))
                    .expiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES))
                    .used(false)
                    .build());

            String link = frontendBaseUrl + "/auth/reset-password?token=" + rawToken;
            String prenom = user.getPrenom() != null ? user.getPrenom() : "";
            try {
                emailService.send(user.getEmail(), "Réinitialisation de votre mot de passe — Domivo",
                        EmailTemplates.passwordReset(prenom, link));
            } catch (Exception e) {
                log.warn("Échec d'envoi de l'email de réinitialisation à {} : {}", email, e.getMessage());
            }
        });
    }

    /** Applique le nouveau mot de passe à partir d'un token valide. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest("WEAK_PASSWORD", "Le mot de passe doit contenir au moins 8 caractères.");
        }

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", "Lien de réinitialisation invalide."));

        if (token.isUsed()) {
            throw ApiException.badRequest("TOKEN_USED", "Ce lien a déjà été utilisé.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("TOKEN_EXPIRED", "Ce lien a expiré — refaites une demande.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.badRequest("USER_NOT_FOUND", "Compte introuvable."));

        user.setMotDePasseHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        // Sécurité : changer le mot de passe déconnecte tous les appareils.
        sessionService.revokeAll(user.getId(), null);

        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        try {
            emailService.send(user.getEmail(), "Votre mot de passe a été modifié — Domivo",
                    EmailTemplates.passwordChanged(prenom));
        } catch (Exception e) {
            log.warn("Échec d'envoi de la confirmation de changement de mot de passe : {}", e.getMessage());
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
