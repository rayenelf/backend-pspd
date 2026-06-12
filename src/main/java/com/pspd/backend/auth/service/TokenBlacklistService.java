package com.pspd.backend.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * Révocation de JWT via Redis (#2).
 * Les JWT sont stateless : pour les invalider (logout), on stocke leur empreinte
 * dans une blacklist Redis avec un TTL = durée de vie restante du token.
 * Une fois le token expiré, l'entrée disparaît automatiquement (pas de fuite mémoire).
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private static final String PREFIX = "bl:";
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Ajoute un token à la blacklist jusqu'à sa date d'expiration. */
    public void blacklist(String token) {
        if (token == null || token.isBlank()) return;
        long ttlSeconds = remainingSeconds(token);
        if (ttlSeconds <= 0) return; // déjà expiré → inutile
        redis.opsForValue().set(PREFIX + fingerprint(token), "1", Duration.ofSeconds(ttlSeconds));
    }

    /** Vrai si le token a été révoqué. */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + fingerprint(token)));
    }

    /** Empreinte SHA-256 (on ne stocke jamais le token en clair dans Redis). */
    private String fingerprint(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Secondes restantes avant expiration (claim exp du JWT). 0 si illisible/expiré. */
    private long remainingSeconds(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = mapper.readTree(payload).get("exp").asLong();
            long now = System.currentTimeMillis() / 1000;
            return Math.max(0, exp - now);
        } catch (Exception e) {
            return 0;
        }
    }
}
