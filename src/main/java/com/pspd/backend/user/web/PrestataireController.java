package com.pspd.backend.user.web;

import com.pspd.backend.user.dto.UpdatePrestataireRequest;
import com.pspd.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestataires")
@RequiredArgsConstructor
public class PrestataireController {

    private final UserService userService;

    /** Mise à jour du profil professionnel. Nécessite JwtAuthenticationFilter (B5). */
    @PatchMapping("/me")
    public ResponseEntity<Void> updateMe(
            @Valid @RequestBody UpdatePrestataireRequest req,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(401).build();
        userService.updatePrestataire(authentication.getName(), req);
        return ResponseEntity.noContent().build();
    }
}
