package com.pspd.backend.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * « Se souvenir de cet appareil » (#4).
 * Après une 2FA réussie, on émet un device-token (30 j). Tant qu'il est présent
 * et valide, le login saute le challenge 2FA pour cet utilisateur.
 *
 * Clé Redis : trusted_device:{tokenHash} → userId (TTL 30 j).
 */
@Service
public class DeviceTrustService {

    private static final Duration TTL = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public DeviceTrustService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Émet un device-token de confiance pour cet utilisateur et le renvoie (brut). */
    public String trust(String userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(key(raw), userId, TTL);
        return raw;
    }

    /** Vrai si le device-token est valide pour cet utilisateur. */
    public boolean isTrusted(String userId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;
        String owner = redis.opsForValue().get(key(rawToken));
        return userId.equals(owner);
    }

    private String key(String rawToken) {
        return "trusted_device:" + sha256(rawToken);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
