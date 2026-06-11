package com.pspd.backend.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception métier portant un statut HTTP + un code applicatif.
 * À lancer depuis les services pour produire une {@link ErrorResponse} cohérente.
 * Exemples : {@code throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Cet email est déjà utilisé.");}
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String     code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code   = code;
    }

    // ── Fabriques courantes ──────────────────────────────────────────────────
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}
