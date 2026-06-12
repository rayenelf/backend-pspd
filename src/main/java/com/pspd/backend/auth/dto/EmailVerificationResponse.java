package com.pspd.backend.auth.dto;

public record EmailVerificationResponse(
        boolean success,
        String message,
        String email
) {
    public static EmailVerificationResponse success(String email) {
        return new EmailVerificationResponse(true, "Code de vérification envoyé avec succès", email);
    }

    public static EmailVerificationResponse verified(String email) {
        return new EmailVerificationResponse(true, "Email vérifié avec succès", email);
    }

    public static EmailVerificationResponse error(String message) {
        return new EmailVerificationResponse(false, message, null);
    }
}