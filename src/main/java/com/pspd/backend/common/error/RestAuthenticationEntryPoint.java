package com.pspd.backend.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Point d'entrée d'authentification REST.
 * Sans ce bean, oauth2Login fait une redirection 302 vers la page de login Google
 * pour toute requête non authentifiée. Pour une API on veut un 401 JSON au format unifié.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String path = request.getRequestURI().replace("\"", "");
        String json = "{\"timestamp\":\"" + Instant.now() + "\","
                + "\"status\":401,\"code\":\"UNAUTHORIZED\","
                + "\"message\":\"Authentification requise.\","
                + "\"path\":\"" + path + "\"}";
        response.getWriter().write(json);
    }
}
