package com.pspd.backend.auth.web;

import com.pspd.backend.common.web.RequestUtils;
import com.pspd.backend.auth.service.OAuth2UserProvisioningService;
import com.pspd.backend.auth.service.SecurityNotificationService;
import com.pspd.backend.auth.service.SessionService;
import com.pspd.backend.auth.service.TokenService;
import com.pspd.backend.user.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

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
    private final SessionService sessionService;
    private final SecurityNotificationService securityNotificationService;

    @Value("${app.frontend.oauth-redirect}")
    private String frontendRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        // Extraction des attributs selon le provider (Google vs Facebook — #7).
        String registrationId = (authentication instanceof OAuth2AuthenticationToken t)
                ? t.getAuthorizedClientRegistrationId() : "google";

        String email = principal.getAttribute("email");
        String prenom, nom;
        if ("facebook".equals(registrationId)) {
            String fullName = principal.getAttribute("name");
            String[] parts = fullName != null ? fullName.trim().split("\\s+", 2) : new String[0];
            prenom = parts.length > 0 ? parts[0] : null;
            nom    = parts.length > 1 ? parts[1] : null;
        } else {
            prenom = principal.getAttribute("given_name");
            nom    = principal.getAttribute("family_name");
        }

        // Lire le rôle pré-sélectionné (depuis la page Signup via OAuthInitiateController).
        // Null si l'utilisateur vient de la page Login → findOrCreate défaut CLIENT.
        HttpSession session = request.getSession(false);
        String pendingRole = null;
        if (session != null) {
            pendingRole = (String) session.getAttribute(OAuthInitiateController.SESSION_KEY_PENDING_ROLE);
            session.removeAttribute(OAuthInitiateController.SESSION_KEY_PENDING_ROLE);
        }

        User user = provisioningService.findOrCreate(email, prenom, nom, pendingRole);

        // Session liée à l'appareil (auth avancé #3) + notif nouvelle connexion (#5).
        String device = request.getHeader("User-Agent");
        String ip = RequestUtils.clientIp(request);
        String sid = sessionService.createSession(user.getId(), device, ip);
        securityNotificationService.notifyIfNewDevice(user, device, ip);
        Map<String, Object> claims = Map.of("sid", sid);

        String accessToken  = tokenService.generateAccessToken(user, claims);
        String refreshToken = tokenService.generateRefreshToken(user, claims);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirect)
                .queryParam("token", accessToken)
                .queryParam("refresh", refreshToken)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}
