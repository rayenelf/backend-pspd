package com.pspd.backend.user.web;

import com.pspd.backend.user.dto.UpdateUserRequest;
import com.pspd.backend.user.dto.UserResponse;
import com.pspd.backend.user.service.UserService;
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
}
