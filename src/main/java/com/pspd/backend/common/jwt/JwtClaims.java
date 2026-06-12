package com.pspd.backend.common.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Lecture (sans vérification de signature) des claims d'un JWT.
 * La signature est vérifiée séparément par TokenService.isValid ; ici on se
 * contente d'extraire des claims déjà validés (sid, exp...).
 */
public final class JwtClaims {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtClaims() {}

    public static String getString(String token, String claim) {
        JsonNode node = payload(token);
        return (node != null && node.hasNonNull(claim)) ? node.get(claim).asText() : null;
    }

    public static Long getLong(String token, String claim) {
        JsonNode node = payload(token);
        return (node != null && node.hasNonNull(claim)) ? node.get(claim).asLong() : null;
    }

    private static JsonNode payload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
