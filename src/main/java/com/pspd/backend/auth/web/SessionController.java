package com.pspd.backend.auth.web;

import com.pspd.backend.auth.dto.SessionResponse;
import com.pspd.backend.auth.service.SessionService;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.jwt.JwtClaims;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des sessions/appareils de l'utilisateur connecté (#3).
 *   GET    /api/users/me/sessions             → liste (session courante marquée)
 *   DELETE /api/users/me/sessions/{sid}        → révoque un appareil
 *   POST   /api/users/me/sessions/logout-all   → déconnecte partout (sauf l'appareil courant)
 */
@RestController
@RequestMapping("/api/users/me/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> list(Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(sessionService.list(userId(auth), currentSid(request)));
    }

    @DeleteMapping("/{sid}")
    public ResponseEntity<Void> revoke(@PathVariable String sid, Authentication auth) {
        sessionService.revoke(userId(auth), sid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication auth, HttpServletRequest request) {
        sessionService.revokeAll(userId(auth), currentSid(request));
        return ResponseEntity.noContent().build();
    }

    private String userId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));
        return user.getId();
    }

    private String currentSid(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return JwtClaims.getString(header.substring(7), "sid");
        }
        return null;
    }
}
