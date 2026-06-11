package com.pspd.backend.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Format d'erreur unifié de l'API (RFC 7807 simplifié — doc 01 §8).
 * Exemple :
 * <pre>
 * {
 *   "timestamp": "2026-06-09T10:15:30Z",
 *   "status": 400,
 *   "code": "VALIDATION_ERROR",
 *   "message": "Le champ email est invalide.",
 *   "path": "/api/auth/register",
 *   "errors": [ { "field": "email", "message": "doit être un email valide" } ]
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    Instant       timestamp,
    int           status,
    String        code,
    String        message,
    String        path,
    List<FieldError> errors
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path, null);
    }

    public static ErrorResponse of(int status, String code, String message, String path, List<FieldError> errors) {
        return new ErrorResponse(Instant.now(), status, code, message, path, errors);
    }
}
