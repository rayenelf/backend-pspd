package com.pspd.backend.auth.web;

import com.pspd.backend.common.web.RequestUtils;
import com.pspd.backend.auth.dto.*;
import com.pspd.backend.auth.service.DeviceTrustService;
import com.pspd.backend.auth.service.SecurityNotificationService;
import com.pspd.backend.auth.service.SessionService;
import com.pspd.backend.auth.service.TwoFactorService;
import com.pspd.backend.auth.service.TwoFactorService.VerifyResult;
import com.pspd.backend.auth.service.TokenService;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TwoFactorController {

    private final TwoFactorService            twoFactorService;
    private final UserRepository              userRepository;
    private final TokenService                tokenService;
    private final SessionService              sessionService;
    private final DeviceTrustService          deviceTrustService;
    private final SecurityNotificationService securityNotificationService;

    @Value("${app.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    /**
     * POST /api/auth/2fa/send — envoie (ou renvoie) un OTP à l'utilisateur.
     * Endpoint public. Retourne toujours 200 pour ne pas révéler l'existence d'un compte.
     */
    @PostMapping("/api/auth/2fa/send")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email());
        userOpt.filter(User::isDoubleAuthActive)
               .ifPresent(twoFactorService::generateAndSendOtp);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/auth/2fa/verify — valide le code OTP, crée une session et renvoie les tokens.
     * Si rememberDevice=true, renvoie aussi un device-token (skip 2FA 30 j sur cet appareil).
     */
    @PostMapping("/api/auth/2fa/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody Verify2faRequest req,
                                                  HttpServletRequest request) {
        VerifyResult result = twoFactorService.verify(req.email(), req.code());

        return switch (result.status()) {
            case SUCCESS -> {
                User user = result.user();
                String device = request.getHeader("User-Agent");
                String ip = RequestUtils.clientIp(request);

                String sid = sessionService.createSession(user.getId(), device, ip);
                securityNotificationService.notifyIfNewDevice(user, device, ip);
                Map<String, Object> claims = Map.of("sid", sid);

                String deviceToken = req.rememberDevice() ? deviceTrustService.trust(user.getId()) : null;
                log.info("[AUDIT] 2FA vérifiée pour userId={} (rememberDevice={})", user.getId(), req.rememberDevice());

                yield ResponseEntity.ok(new AuthResponse(
                    tokenService.generateAccessToken(user, claims),
                    tokenService.generateRefreshToken(user, claims),
                    accessTokenMinutes * 60,
                    user.getRole().name(),
                    deviceToken
                ));
            }
            case EXPIRED           -> throw ApiException.badRequest(
                "OTP_EXPIRED", "Code OTP expiré — demandez un nouveau code.");
            case TOO_MANY_ATTEMPTS -> throw new ApiException(
                HttpStatus.TOO_MANY_REQUESTS, "OTP_TOO_MANY_ATTEMPTS",
                "Trop de tentatives incorrectes — demandez un nouveau code.");
            case INVALID_CODE      -> throw ApiException.badRequest(
                "OTP_INVALID", "Code incorrect — vérifiez votre code.");
            default                -> throw ApiException.badRequest(
                "OTP_NONE", "Aucun OTP en attente pour cet email.");
        };
    }

    /** POST /api/users/me/2fa — active/désactive la 2FA + notifie par email (#5). */
    @PostMapping("/api/users/me/2fa")
    public ResponseEntity<Void> toggle2fa(
            @RequestBody Toggle2faRequest req,
            Authentication auth) {
        String email = auth.getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setDoubleAuthActive(req.active());
            userRepository.save(user);
            securityNotificationService.notify2faChanged(user, req.active());
            log.info("[AUDIT] 2FA {} pour userId={}", req.active() ? "activée" : "désactivée", user.getId());
        });
        return ResponseEntity.noContent().build();
    }
}
