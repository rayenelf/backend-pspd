package com.pspd.backend.common.config;

import com.pspd.backend.auth.web.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // TODO B5 (collègue) : injecter JwtAuthenticationFilter ici
    private final OAuth2SuccessHandler oauth2SuccessHandler; // B8 (Majd)

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Session uniquement pour le handshake OAuth2 (state) ; l'API reste
            // stateless via JWT. IF_REQUIRED autorise la session le temps de la
            // redirection Google ↔ callback.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // ── Endpoints publics Auth ──────────────────────────────
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/2fa/send",    // envoi OTP (login flow + renvoi)
                    "/api/auth/2fa/verify",  // vérification OTP → tokens
                    "/api/auth/refresh"
                ).permitAll()
                // ── Callbacks OAuth2 + initiation avec rôle ────────────
                .requestMatchers(
                    "/login/oauth2/code/**",
                    "/oauth2/**",
                    "/api/auth/oauth2/google"       // OAuthInitiateController (signup avec rôle)
                ).permitAll()
                // ── Tout le reste requiert une authentification ─────────
                .anyRequest().authenticated()
            )
            // ── B8 (Majd) : login OAuth2 Google → JWT émis par le handler ──
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2SuccessHandler)
            );

        // TODO B5 (collègue) : http.addFilterBefore(jwtAuthenticationFilter, ...)
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
