package com.pspd.backend.booking.dto;

import com.pspd.backend.booking.domain.Reservation;
import com.pspd.backend.booking.domain.StatutReservation;
import com.pspd.backend.booking.domain.TypeReservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Vue API d'une réservation — contrat partagé consommé par les deux frontends
 * (« Mes réservations » client / « Mes missions » prestataire).
 */
public record ReservationResponse(
    String id,
    String clientId,
    String prestataireId,
    String serviceId,
    String adresseId,
    TypeReservation type,
    StatutReservation statut,
    LocalDate dateService,
    LocalTime heureService,
    BigDecimal prixConvenu,
    LocalDateTime creeLe
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
            r.getId(), r.getClientId(), r.getPrestataireId(), r.getServiceId(),
            r.getAdresseId(), r.getType(), r.getStatut(), r.getDateService(),
            r.getHeureService(), r.getPrixConvenu(), r.getCreeLe());
    }
}
