package com.pspd.backend.auth.web;

import com.pspd.backend.auth.service.OAuth2UserProvisioningService;
import com.pspd.backend.auth.service.TokenService;
import com.pspd.backend.user.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Déclenché après une authentification OAuth2 Google réussie.
 * Étape B8 — tâche Majd.
 *
 * Flux : extrait l'email du profil Google → findOrCreate du User →
 * émet access + refresh tokens → redirige vers le frontend avec les tokens.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2UserProvisioningService provisioningService;
    private final TokenService tokenService;

    @Value("${app.frontend.oauth-redirect}")
    private String frontendRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        String email  = principal.getAttribute("email");
        String prenom = principal.getAttribute("given_name");
        String nom    = principal.getAttribute("family_name");

        // Lire le rôle pré-sélectionné (depuis la page Signup via OAuthInitiateController).
        // Null si l'utilisateur vient de la page Login → findOrCreate défaut CLIENT.
        HttpSession session = request.getSession(false);
        String pendingRole = null;
        if (session != null) {
            pendingRole = (String) session.getAttribute(OAuthInitiateController.SESSION_KEY_PENDING_ROLE);
            session.removeAttribute(OAuthInitiateController.SESSION_KEY_PENDING_ROLE);
        }

        User user = provisioningService.findOrCreate(email, prenom, nom, pendingRole);

        String accessToken  = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirect)
                .queryParam("token", accessToken)
                .queryParam("refresh", refreshToken)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}
