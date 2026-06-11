package com.pspd.backend.auth.service;

import com.pspd.backend.auth.domain.EmailVerificationToken;
import com.pspd.backend.auth.repository.EmailVerificationTokenRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.mail.EmailService;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Vérification d'email : génère un lien à usage unique (TTL 24 h), l'envoie,
 * et valide le token reçu. Les comptes OAuth2 sont vérifiés d'office (email Google fiable).
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int    TTL_HOURS = 24;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository                   userRepository;
    private final EmailService                     emailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /** Crée un token et envoie l'email de vérification. */
    @Transactional
    public void sendVerification(User user) {
        tokenRepository.deleteByUserId(user.getId());

        String rawToken = randomToken();
        tokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(TTL_HOURS))
                .used(false)
                .build());

        String link = frontendBaseUrl + "/auth/verify-email?token=" + rawToken;
        emailService.send(user.getEmail(), "Confirmez votre adresse email — Domivo", """
                Bonjour %s,

                Confirmez votre adresse email en cliquant sur ce lien (valable 24 h) :
                %s

                Si vous n'êtes pas à l'origine de cette inscription, ignorez ce message.
                """.formatted(user.getPrenom() != null ? user.getPrenom() : "", link));
    }

    /** Renvoie un email de vérification si le compte existe et n'est pas déjà vérifié. */
    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email)
                .filter(u -> !u.isEmailVerifie())
                .ifPresent(this::sendVerification);
        // Pas d'erreur si l'email n'existe pas / déjà vérifié → on ne révèle rien.
    }

    /** Valide le token et marque l'email comme vérifié. */
    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", "Lien de vérification invalide."));

        if (token.isUsed()) {
            throw ApiException.badRequest("TOKEN_USED", "Ce lien a déjà été utilisé.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("TOKEN_EXPIRED", "Ce lien a expiré — demandez-en un nouveau.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.badRequest("USER_NOT_FOUND", "Compte introuvable."));

        user.setEmailVerifie(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
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
