package com.pspd.backend.user.web;

import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.dto.UserResponse;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints du profil courant.
 * GET /api/users/me — disponible dès que le JwtAuthenticationFilter (B5 — collègue)
 * est en place et peuple le SecurityContext.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Retourne le profil de l'utilisateur connecté.
     * L'email est extrait du SecurityContext (peuplé par JwtAuthenticationFilter — B5).
     * Retourne 401 si non authentifié, 404 si l'email du token ne correspond à aucun compte.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}
