package com.pspd.backend.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspd.backend.auth.service.TokenService;
import com.pspd.backend.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * ⚠️ TEMPORAIRE — À SUPPRIMER après le merge de la branche du collègue (B4 : TokenServiceImpl).
 *
 * Fournit une implémentation minimale de {@link TokenService} pour pouvoir tester
 * le flux OAuth2 Google de bout en bout AVANT que le vrai service JWT (JJWT) soit livré.
 *
 * - Génère un vrai JWT HS256 (compatible avec la future implémentation JJWT si le
 *   secret {@code app.jwt.secret} est identique).
 * - Annoté {@code @ConditionalOnMissingBean} : si un autre bean TokenService existe
 *   (le TokenServiceImpl du collègue), CE bean n'est pas créé → aucun conflit.
 */
@Configuration
public class StubTokenServiceConfig {

    @Bean
    @ConditionalOnMissingBean(TokenService.class)
    public TokenService stubTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessMinutes,
            @Value("${app.jwt.refresh-token-days}") long refreshDays) {

        return new TokenService() {

            private final ObjectMapper mapper = new ObjectMapper();
            private final Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();

            @Override
            public String generateAccessToken(User user) {
                long exp = System.currentTimeMillis() / 1000 + accessMinutes * 60;
                // HashMap pour autoriser les valeurs null (prenom/nom peuvent être null)
                java.util.Map<String, Object> claims = new java.util.HashMap<>();
                claims.put("sub",    user.getEmail());
                claims.put("role",   user.getRole().name());
                claims.put("uid",    user.getId());
                claims.put("exp",    exp);
                claims.put("prenom",    user.getPrenom());
                claims.put("nom",       user.getNom());
                claims.put("telephone", user.getTelephone());
                return buildJwt(claims);
            }

            @Override
            public String generateRefreshToken(User user) {
                long exp = System.currentTimeMillis() / 1000 + refreshDays * 24 * 3600;
                return buildJwt(Map.of(
                        "sub", user.getEmail(),
                        "type", "refresh",
                        "exp", exp
                ));
            }

            @Override
            public String extractEmail(String token) {
                try {
                    String payloadJson = new String(
                            Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                            StandardCharsets.UTF_8);
                    return mapper.readTree(payloadJson).get("sub").asText();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public boolean isValid(String token) {
                try {
                    String[] parts = token.split("\\.");
                    if (parts.length != 3) return false;
                    String signed = sign(parts[0] + "." + parts[1]);
                    if (!signed.equals(parts[2])) return false;
                    long exp = mapper.readTree(
                            Base64.getUrlDecoder().decode(parts[1])).get("exp").asLong();
                    return exp > System.currentTimeMillis() / 1000;
                } catch (Exception e) {
                    return false;
                }
            }

            // ── helpers ─────────────────────────────────────────────
            private String buildJwt(Map<String, Object> claims) {
                try {
                    String header = b64.encodeToString(
                            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
                    String payload = b64.encodeToString(
                            mapper.writeValueAsBytes(claims));
                    String signature = sign(header + "." + payload);
                    return header + "." + payload + "." + signature;
                } catch (Exception e) {
                    throw new IllegalStateException("Stub JWT generation failed", e);
                }
            }

            private String sign(String data) throws Exception {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                return b64.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
            }
        };
    }
}
