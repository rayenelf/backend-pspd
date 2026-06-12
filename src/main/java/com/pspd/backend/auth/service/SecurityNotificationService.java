package com.pspd.backend.auth.service;

import com.pspd.backend.common.mail.EmailService;
import com.pspd.backend.common.mail.EmailTemplates;
import com.pspd.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * Notifications de sécurité par email (#5) :
 * - nouvelle connexion depuis un appareil jamais vu,
 * - activation / désactivation de la 2FA.
 * Les empreintes d'appareils connus sont mémorisées dans Redis (90 j).
 */
@Service
@RequiredArgsConstructor
public class SecurityNotificationService {

    private static final Duration KNOWN_TTL = Duration.ofDays(90);

    private final EmailService emailService;
    private final StringRedisTemplate redis;

    /** Envoie un email si la connexion vient d'un appareil/IP inconnu. */
    public void notifyIfNewDevice(User user, String device, String ip) {
        String fp = fingerprint(device, ip);
        String key = "known_devices:" + user.getId();
        Long added = redis.opsForSet().add(key, fp);
        redis.expire(key, KNOWN_TTL);
        boolean isNew = added != null && added > 0;
        if (isNew) {
            emailService.send(user.getEmail(), "Nouvelle connexion à votre compte Domivo",
                    EmailTemplates.newLogin(displayName(user), device, ip));
        }
    }

    /** Notifie l'activation ou la désactivation de la 2FA. */
    public void notify2faChanged(User user, boolean active) {
        emailService.send(user.getEmail(),
                active ? "Double authentification activée" : "Double authentification désactivée",
                EmailTemplates.twoFactorChanged(displayName(user), active));
    }

    private String displayName(User user) {
        return user.getPrenom() != null ? user.getPrenom() : user.getEmail();
    }

    private String fingerprint(String device, String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(
                    digest.digest((device + "|" + ip).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return device + "|" + ip;
        }
    }
}
