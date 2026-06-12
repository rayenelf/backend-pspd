package com.pspd.backend.auth.service;

import com.pspd.backend.user.domain.User;

import java.util.Map;

/**
 * Contrat partagé entre AuthService (login/register) et OAuth2SuccessHandler.
 * Implémenté dans auth.service.TokenServiceImpl (étape B4).
 */
public interface TokenService {

    /** Génère un access token JWT (durée 15 min, claims: sub, role, exp). */
    String generateAccessToken(User user);

    /** Génère un refresh token opaque (durée 7 jours). */
    String generateRefreshToken(User user);

    /** Extrait l'email (sub) depuis un access token. */
    String extractEmail(String token);

    /** Vérifie la signature et la date d'expiration du token. */
    boolean isValid(String token);

    /**
     * Variantes avec claims supplémentaires (ex. sid de session — auth avancé).
     * Méthodes {@code default} pour rester rétro-compatible : une implémentation
     * existante qui ne les surcharge pas ignore simplement les claims en plus.
     */
    default String generateAccessToken(User user, Map<String, Object> extraClaims) {
        return generateAccessToken(user);
    }

    default String generateRefreshToken(User user, Map<String, Object> extraClaims) {
        return generateRefreshToken(user);
    }
}
