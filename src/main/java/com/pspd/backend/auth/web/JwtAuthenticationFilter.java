package com.pspd.backend.auth.web;

import com.pspd.backend.auth.service.SessionService;
import com.pspd.backend.auth.service.TokenBlacklistService;
import com.pspd.backend.auth.service.TokenService;
import com.pspd.backend.common.jwt.JwtClaims;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtre d'authentification JWT (B5).
 * Lit l'en-tête {@code Authorization: Bearer <jwt>}, valide le token via {@link TokenService},
 * charge l'utilisateur et place l'authentification dans le {@link SecurityContextHolder}.
 *
 * Ne bloque rien : en l'absence de token (ou token invalide), la chaîne continue et
 * la décision revient à SecurityConfig (permitAll vs authenticated).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService          tokenService;
    private final UserRepository        userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionService        sessionService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Le JWT (Bearer) fait TOUJOURS autorité pour l'API : s'il est présent, on
        // IGNORE toute authentification de session héritée (ex. cookie JSESSIONID
        // OAuth2 résiduel après un login Google) en la remplaçant par celle du token.
        // Sans ça, l'auth de session (principal Google, rôle OIDC) court-circuitait le
        // JWT → 403 sur les endpoints à rôle + déconnexions parasites.
        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            // sid présent → la session doit exister dans Redis (sinon révoquée).
            String sid = JwtClaims.getString(token, "sid");
            boolean sessionOk = (sid == null) || sessionService.exists(sid);

            if (sessionOk && tokenService.isValid(token) && !tokenBlacklistService.isBlacklisted(token)) {
                String email = tokenService.extractEmail(token);
                Optional<User> userOpt = (email != null)
                        ? userRepository.findByEmail(email)
                        : Optional.empty();

                if (sid != null) sessionService.touch(sid);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    var authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null, authorities);
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // Token présent mais utilisateur introuvable → on n'hérite pas d'une
                    // auth de session : la requête doit être traitée comme non authentifiée.
                    SecurityContextHolder.clearContext();
                }
            } else {
                // Token présent mais invalide/révoqué → idem : pas d'auth héritée.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
