package com.pspd.backend.auth.controller;

import com.pspd.backend.auth.dto.ForgotPasswordRequest;
import com.pspd.backend.auth.dto.PasswordResetResponse;
import com.pspd.backend.auth.dto.ResetPasswordRequest;
import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.auth.dto.RegisterResponse;
import com.pspd.backend.auth.service.AuthService;
import com.pspd.backend.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pspd.backend.auth.dto.LoginRequest;
import com.pspd.backend.auth.dto.LoginResponse;
import com.pspd.backend.auth.dto.RefreshRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest req) {
        // DEPRECATED: Cette méthode sera supprimée. Utiliser /send-verification + /verify-email
        // Pour maintenir la compatibilité temporaire, on crée directement le compte sans vérification
        RegisterResponse resp = authService.registerDirect(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        LoginResponse resp = authService.authenticate(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(passwordResetService.requestReset(req.email()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(passwordResetService.resetPassword(req.token(), req.motDePasse()));
    }
}
