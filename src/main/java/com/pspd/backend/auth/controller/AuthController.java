package com.pspd.backend.auth.controller;

import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.auth.dto.RegisterResponse;
import com.pspd.backend.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest req) {
        RegisterResponse resp = authService.register(req);
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

    /**
     * Déconnexion. En Phase 1 (sans Redis), le serveur ne révoque pas le JWT :
     * le client purge ses tokens. La révocation par blacklist arrivera en Phase 2.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null) {
            log.info("[AUDIT] Déconnexion de {}", authentication.getName());
        }
        return ResponseEntity.noContent().build();
    }
}
