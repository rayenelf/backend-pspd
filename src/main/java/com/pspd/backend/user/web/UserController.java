package com.pspd.backend.user.web;

import com.pspd.backend.user.dto.ChangePasswordRequest;
import com.pspd.backend.user.dto.DeleteAccountRequest;
import com.pspd.backend.user.dto.UpdateUserRequest;
import com.pspd.backend.user.dto.UserResponse;
import com.pspd.backend.user.service.UserService;
import com.pspd.backend.common.jwt.JwtClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Profil de l'utilisateur connecté. Nécessite JwtAuthenticationFilter (B5). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.getByEmail(authentication.getName()));
    }

    /** Mise à jour des infos personnelles (prenom, nom, telephone). */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateUserRequest req,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.updateUser(authentication.getName(), req));
    }

    /** Changement de mot de passe (utilisateur connecté). Déconnecte les autres appareils. */
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication,
            HttpServletRequest request) {
        String currentSid = null;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            currentSid = JwtClaims.getString(header.substring(7), "sid");
        }
        userService.changePassword(authentication.getName(), req.currentPassword(), req.newPassword(), currentSid);
        return ResponseEntity.noContent().build();
    }

    /** Suppression de compte (RGPD, #6) : anonymise + révoque toutes les sessions. */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @RequestBody(required = false) DeleteAccountRequest req,
            Authentication authentication) {
        String password = req != null ? req.password() : null;
        userService.deleteAccount(authentication.getName(), password);
        return ResponseEntity.noContent().build();
    }
}
