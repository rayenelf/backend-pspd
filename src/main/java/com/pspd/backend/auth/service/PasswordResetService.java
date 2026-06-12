package com.pspd.backend.auth.service;

import com.pspd.backend.auth.dto.PasswordResetResponse;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.password-reset-url:http://localhost:5173/auth/reset-password}")
    private String resetBaseUrl;

    private final Map<String, ResetToken> tokens = new ConcurrentHashMap<>();

    public PasswordResetResponse requestReset(String email) {
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("EMAIL_REQUIRED", "Email requis");
        }

        cleanupExpiredTokens();

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        userOpt.ifPresent(this::createAndSendResetLink);

        return new PasswordResetResponse("Si un compte correspond à cet email, un lien de réinitialisation a été envoyé.");
    }

    public PasswordResetResponse resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest("TOKEN_REQUIRED", "Le lien de réinitialisation est invalide.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest("PASSWORD_TOO_SHORT", "Le mot de passe doit contenir au moins 8 caractères.");
        }

        cleanupExpiredTokens();

        ResetToken resetToken = tokens.remove(token);
        if (resetToken == null || resetToken.isExpired()) {
            throw ApiException.badRequest("TOKEN_INVALID", "Le lien de réinitialisation est invalide ou expiré.");
        }

        User user = userRepository.findByEmail(resetToken.email())
                .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", "Le lien de réinitialisation est invalide ou expiré."));

        user.setMotDePasseHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return new PasswordResetResponse("Votre mot de passe a été réinitialisé.");
    }

    private void createAndSendResetLink(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new ResetToken(user.getEmail(), Instant.now().plus(TOKEN_TTL)));

        String resetLink = UriComponentsBuilder.fromUriString(resetBaseUrl)
                .queryParam("token", token)
                .build(true)
                .toUriString();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Réinitialisation de votre mot de passe");
            message.setText("Bonjour,\n\nCliquez sur ce lien pour réinitialiser votre mot de passe :\n" + resetLink +
                    "\n\nCe lien expire dans 30 minutes.\nSi vous n'êtes pas à l'origine de cette demande, ignorez ce message.");
            mailSender.send(message);
            log.info("[password-reset] Email envoyé à {}", user.getEmail());
        } catch (MailException ex) {
            log.warn("[password-reset] Envoi email impossible pour {} ; lien de dev: {}", user.getEmail(), resetLink);
        }
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record ResetToken(String email, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}