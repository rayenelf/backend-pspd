package com.pspd.backend.auth.dto;

// Réponse renvoyée après login réussi ou vérification 2FA réussie.
// Sera également utilisée par le collègue (endpoint login B6).
public record AuthResponse(
    String accessToken,
    String refreshToken,
    long   expiresIn,   // secondes
    String role
) {}
