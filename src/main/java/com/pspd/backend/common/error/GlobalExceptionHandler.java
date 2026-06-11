package com.pspd.backend.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Handler d'erreurs global (B10 — Majd).
 * Convertit toute exception en {@link ErrorResponse} au format unifié (doc 01 §8).
 * Journalise les 5xx ; ne loggue jamais de données sensibles (mot de passe, OTP, token).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Exceptions métier explicites (statut + code portés par l'exception). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getStatus())
            .body(ErrorResponse.of(ex.getStatus().value(), ex.getCode(), ex.getMessage(), req.getRequestURI()));
    }

    /** Validation des DTO (@Valid sur @RequestBody) → 400 avec le détail par champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldError)
            .toList();
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(400, "VALIDATION_ERROR", "Un ou plusieurs champs sont invalides.",
                req.getRequestURI(), fields));
    }

    /** Validation sur paramètres simples (@RequestParam, @PathVariable). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(400, "VALIDATION_ERROR", ex.getMessage(), req.getRequestURI()));
    }

    /** Corps de requête illisible / JSON malformé. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(400, "MALFORMED_REQUEST", "Corps de requête invalide ou illisible.",
                req.getRequestURI()));
    }

    /** Accès refusé par @PreAuthorize / ownership. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.of(403, "FORBIDDEN", "Accès refusé à cette ressource.", req.getRequestURI()));
    }

    /** Échec d'authentification (token absent/invalide au niveau méthode). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(401, "UNAUTHORIZED", "Authentification requise.", req.getRequestURI()));
    }

    /**
     * Exceptions métier levées via ResponseStatusException (ex. AuthService du collègue :
     * 409 email déjà utilisé, 401 identifiants invalides…). On respecte le statut porté
     * et on ne loggue PAS de stack trace : ce sont des réponses métier légitimes, pas des 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        int status = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : "Requête refusée.";
        return ResponseEntity.status(status)
            .body(ErrorResponse.of(status, "BUSINESS_ERROR", message, req.getRequestURI()));
    }

    /** Filet de sécurité — toute exception non gérée → 500 (loggée, message générique). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Erreur non gérée sur {} : {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(500, "INTERNAL_ERROR", "Une erreur interne est survenue.", req.getRequestURI()));
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}
