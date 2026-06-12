package com.pspd.backend.auth.controller;

import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.auth.dto.RegisterResponse;
import com.pspd.backend.auth.service.AuthService;
import com.pspd.backend.auth.service.EmailVerificationService;
import com.pspd.backend.auth.service.PasswordResetService;
import com.pspd.backend.auth.service.TokenBlacklistService;
import com.pspd.backend.auth.service.SessionService;
import com.pspd.backend.common.jwt.JwtClaims;
import com.pspd.backend.common.web.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pspd.backend.auth.dto.LoginRequest;
import com.pspd.backend.auth.dto.LoginResponse;
import com.pspd.backend.auth.dto.RefreshRequest;
import com.pspd.backend.auth.dto.VerifyEmailRequest;
import com.pspd.backend.auth.dto.EmailRequest;
import com.pspd.backend.auth.dto.ResetPasswordRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionService sessionService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest req) {
        RegisterResponse resp = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest req,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            HttpServletRequest request) {
        String device = request.getHeader("User-Agent");
        String ip = RequestUtils.clientIp(request);
        return ResponseEntity.ok(authService.authenticate(req, device, ip, deviceToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    /**
     * Déconnexion avec révocation réelle (#2) : l'access token (en-tête) et le
     * refresh token (corps, optionnel) sont blacklistés dans Redis jusqu'à expiration.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshRequest body,
            Authentication authentication) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String access = authHeader.substring(7);
            tokenBlacklistService.blacklist(access);
            sessionService.revokeBySid(JwtClaims.getString(access, "sid")); // termine la session
        }
        if (body != null && body.getRefreshToken() != null) {
            tokenBlacklistService.blacklist(body.getRefreshToken());
        }
        if (authentication != null) {
            log.info("[AUDIT] Déconnexion de {}", authentication.getName());
        }
        return ResponseEntity.noContent().build();
    }

    /** Valide le lien de vérification d'email reçu par mail. */
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        emailVerificationService.verify(req.token());
        return ResponseEntity.noContent().build();
    }

    /** Renvoie un lien de vérification (réponse 204 quoi qu'il arrive — pas de fuite d'info). */
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest req) {
        emailVerificationService.resend(req.email());
        return ResponseEntity.noContent().build();
    }

    /** Mot de passe oublié : envoie un lien de réinitialisation (204 quoi qu'il arrive). */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest req) {
        passwordResetService.requestReset(req.email());
        return ResponseEntity.noContent().build();
    }

    /** Applique le nouveau mot de passe à partir du lien reçu par email. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
