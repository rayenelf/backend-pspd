package com.pspd.backend.auth.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Endpoint d'initiation OAuth2 avec rôle pré-sélectionné.
 *
 * Utilisé par la page Signup : l'utilisateur choisit CLIENT ou PRESTATAIRE,
 * le frontend appelle /api/auth/oauth2/google?role=CLIENT (ou PRESTATAIRE),
 * ce contrôleur stocke le rôle en session HTTP puis redirige vers Google.
 * L'OAuth2SuccessHandler lit le rôle depuis la session pour les nouveaux comptes.
 *
 * Sur la page Login (utilisateur existant), on va directement sur
 * /oauth2/authorization/google — ce contrôleur n'est pas appelé.
 */
@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuthInitiateController {

    public static final String SESSION_KEY_PENDING_ROLE = "oauth2_pending_role";

    @GetMapping("/google")
    public void initiateGoogleAuth(
            @RequestParam(defaultValue = "CLIENT") String role,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        // Accepter uniquement les rôles valides côté signup
        String validatedRole = "PRESTATAIRE".equalsIgnoreCase(role) ? "PRESTATAIRE" : "CLIENT";
        session.setAttribute(SESSION_KEY_PENDING_ROLE, validatedRole);

        response.sendRedirect("/oauth2/authorization/google");
    }
}
