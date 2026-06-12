package com.pspd.backend.common.config;

import com.pspd.backend.auth.web.JwtAuthenticationFilter;
import com.pspd.backend.auth.web.OAuth2SuccessHandler;
import com.pspd.backend.common.error.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler         oauth2SuccessHandler;    // B8 (Majd)
    private final JwtAuthenticationFilter      jwtAuthenticationFilter; // B5
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                    "/api/auth/refresh",
                    "/api/auth/verify-email",        // vérification d'email
                    "/api/auth/resend-verification", // renvoi du lien
                    "/api/auth/forgot-password",     // demande de réinitialisation
                    "/api/auth/reset-password"       // application du nouveau mdp
                ).permitAll()
                // ── Callbacks OAuth2 + initiation avec rôle ────────────
                .requestMatchers(
                    "/login/oauth2/code/**",
                    "/oauth2/**",
                    "/api/auth/oauth2/**"           // OAuthInitiateController (signup avec rôle, google/facebook)
                ).permitAll()
                // ── Catalogue & recherche publics (Epic B) — lecture seule ──
                // (POST/PATCH/DELETE catégories & services restent ADMIN via @PreAuthorize)
                .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/search").permitAll()
                // ── Tout le reste requiert une authentification ─────────
                .anyRequest().authenticated()
            )
            // ── B8 (Majd) : login OAuth2 Google → JWT émis par le handler ──
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2SuccessHandler)
            )
            // ── API : 401 JSON au lieu d'une redirection 302 vers le login OAuth ──
            .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
            // ── B5 : filtre JWT avant l'authentification par formulaire ──
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** CORS — liste blanche du frontend (dev). En prod, ajouter le domaine réel. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
