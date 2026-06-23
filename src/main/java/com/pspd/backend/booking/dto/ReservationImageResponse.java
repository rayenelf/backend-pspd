package com.pspd.backend.booking.dto;

import com.pspd.backend.booking.domain.ReservationImage;

/**
 * Métadonnées d'une image de travail. Le binaire est servi par
 * GET /api/reservations/{id}/images/{imageId}/file (flux authentifié).
 */
public record ReservationImageResponse(
    String id,
    String reservationId,
    String url,
    String contentType,
    Integer ordre
) {
    public static ReservationImageResponse from(ReservationImage img) {
        return new ReservationImageResponse(
            img.getId(), img.getReservationId(), img.getUrl(),
            img.getContentType(), img.getOrdre());
    }
}
