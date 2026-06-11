package com.pspd.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse du login email/mot de passe.
 * - Cas normal : accessToken + refreshToken renseignés, twoFactorRequired=false.
 * - Cas 2FA active : tokens null, twoFactorRequired=true → le front redirige vers /auth/2fa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String id;
    private String email;
    private String role;
    private String statutCompte;
    private String accessToken;
    private String refreshToken;
    private boolean twoFactorRequired;

    /** Challenge 2FA : aucun token, l'utilisateur doit saisir l'OTP. */
    public static LoginResponse twoFactorChallenge(String email) {
        LoginResponse r = new LoginResponse();
        r.email = email;
        r.twoFactorRequired = true;
        return r;
    }
}
