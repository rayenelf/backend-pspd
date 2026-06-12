package com.pspd.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// Réponse renvoyée après vérification 2FA réussie.
// deviceToken : présent uniquement si « se souvenir de cet appareil » a été coché (#4).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    String accessToken,
    String refreshToken,
    long   expiresIn,   // secondes
    String role,
    String deviceToken
) {}
