package com.pspd.backend.auth.web;

import com.pspd.backend.auth.dto.*;
import com.pspd.backend.auth.service.TwoFactorService;
import com.pspd.backend.auth.service.TwoFactorService.VerifyResult;
import com.pspd.backend.auth.service.TokenService;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserRepository   userRepository;
    private final TokenService     tokenService;

    @Value("${app.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    /**
     * POST /api/auth/2fa/send — envoie (ou renvoie) un OTP à l'utilisateur.
     * Endpoint public — utilisé lors du login si 2FA active, et pour "Renvoyer".
     * Retourne toujours 200 pour ne pas révéler l'existence d'un compte.
     */
    @PostMapping("/api/auth/2fa/send")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email());
        userOpt.filter(User::isDoubleAuthActive)
               .ifPresent(twoFactorService::generateAndSendOtp);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/auth/2fa/verify — valide le code OTP et renvoie les tokens JWT.
     * Endpoint public (avant authentification complète).
     */
    @PostMapping("/api/auth/2fa/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody Verify2faRequest req) {
        VerifyResult result = twoFactorService.verify(req.email(), req.code());

        return switch (result.status()) {
            case SUCCESS -> {
                User user = result.user();
                yield ResponseEntity.ok(new AuthResponse(
                    tokenService.generateAccessToken(user),
                    tokenService.generateRefreshToken(user),
                    accessTokenMinutes * 60,
                    user.getRole().name()
                ));
            }
            case EXPIRED           -> ResponseEntity.badRequest()
                .body(Map.of("message", "Code OTP expiré — demandez un nouveau code."));
            case TOO_MANY_ATTEMPTS -> ResponseEntity.status(429)
                .body(Map.of("message", "Trop de tentatives incorrectes — demandez un nouveau code."));
            case INVALID_CODE      -> ResponseEntity.badRequest()
                .body(Map.of("message", "Code incorrect — vérifiez votre code."));
            default                -> ResponseEntity.badRequest()
                .body(Map.of("message", "Aucun OTP en attente pour cet email."));
        };
    }

    /**
     * POST /api/users/me/2fa — active ou désactive la 2FA pour l'utilisateur connecté.
     * Endpoint protégé (disponible après B5 — JwtAuthenticationFilter du collègue).
     */
    @PostMapping("/api/users/me/2fa")
    public ResponseEntity<Void> toggle2fa(
            @RequestBody Toggle2faRequest req,
            Authentication auth) {
        String email = auth.getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setDoubleAuthActive(req.active());
            userRepository.save(user);
        });
        return ResponseEntity.noContent().build();
    }
}
