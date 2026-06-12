package com.pspd.backend.auth.service;

import com.pspd.backend.auth.dto.SessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Registre des sessions/appareils dans Redis (#3).
 * Chaque login crée une session (sid) ; le sid est injecté dans le JWT.
 * Supprimer une session invalide instantanément ses tokens (vérifié par le filtre).
 *
 * Clés :
 *   session:{sid}        → hash {userId, device, ip, createdAt, lastSeenAt}
 *   user_sessions:{uid}  → set des sid de l'utilisateur
 */
@Service
public class SessionService {

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public SessionService(StringRedisTemplate redis,
                          @Value("${app.jwt.refresh-token-days}") long refreshDays) {
        this.redis = redis;
        this.ttl = Duration.ofDays(refreshDays);
    }

    private String sessionKey(String sid) { return "session:" + sid; }
    private String userKey(String userId) { return "user_sessions:" + userId; }

    /** Crée une session et renvoie son identifiant (sid). */
    public String createSession(String userId, String device, String ip) {
        String sid = UUID.randomUUID().toString();
        String now = Instant.now().toString();
        redis.opsForHash().putAll(sessionKey(sid), Map.of(
                "userId", userId,
                "device", device == null ? "Inconnu" : device,
                "ip", ip == null ? "" : ip,
                "createdAt", now,
                "lastSeenAt", now
        ));
        redis.expire(sessionKey(sid), ttl);
        redis.opsForSet().add(userKey(userId), sid);
        redis.expire(userKey(userId), ttl);
        return sid;
    }

    /** Vrai si la session existe encore (non révoquée). */
    public boolean exists(String sid) {
        return sid != null && Boolean.TRUE.equals(redis.hasKey(sessionKey(sid)));
    }

    /** Met à jour la date de dernière activité + prolonge le TTL. */
    public void touch(String sid) {
        if (!exists(sid)) return;
        redis.opsForHash().put(sessionKey(sid), "lastSeenAt", Instant.now().toString());
        redis.expire(sessionKey(sid), ttl);
    }

    /** Liste les sessions de l'utilisateur, en marquant la session courante. */
    public List<SessionResponse> list(String userId, String currentSid) {
        Set<String> sids = redis.opsForSet().members(userKey(userId));
        List<SessionResponse> result = new ArrayList<>();
        if (sids == null) return result;
        for (String sid : sids) {
            Map<Object, Object> h = redis.opsForHash().entries(sessionKey(sid));
            if (h.isEmpty()) {
                redis.opsForSet().remove(userKey(userId), sid); // nettoyage des sid expirés
                continue;
            }
            result.add(new SessionResponse(
                    sid,
                    (String) h.get("device"),
                    (String) h.get("ip"),
                    (String) h.get("createdAt"),
                    (String) h.get("lastSeenAt"),
                    sid.equals(currentSid)
            ));
        }
        return result;
    }

    /** Révoque une session par son sid (lit le propriétaire depuis le hash). */
    public void revokeBySid(String sid) {
        if (sid == null) return;
        Object userId = redis.opsForHash().get(sessionKey(sid), "userId");
        redis.delete(sessionKey(sid));
        if (userId != null) redis.opsForSet().remove(userKey(userId.toString()), sid);
    }

    /** Révoque une session précise (si elle appartient bien à l'utilisateur). */
    public void revoke(String userId, String sid) {
        Object owner = redis.opsForHash().get(sessionKey(sid), "userId");
        if (owner != null && owner.equals(userId)) {
            redis.delete(sessionKey(sid));
            redis.opsForSet().remove(userKey(userId), sid);
        }
    }

    /** Révoque toutes les sessions de l'utilisateur, sauf éventuellement la courante. */
    public void revokeAll(String userId, String exceptSid) {
        Set<String> sids = redis.opsForSet().members(userKey(userId));
        if (sids == null) return;
        for (String sid : sids) {
            if (sid.equals(exceptSid)) continue;
            redis.delete(sessionKey(sid));
            redis.opsForSet().remove(userKey(userId), sid);
        }
    }
}
