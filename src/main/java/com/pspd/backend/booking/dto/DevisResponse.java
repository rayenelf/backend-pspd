package com.pspd.backend.booking.dto;

import com.pspd.backend.booking.domain.Devis;
import com.pspd.backend.booking.domain.StatutDevis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vue API d'un devis — consommée par le front prestataire (« mon devis envoyé »)
 * et le front client (« devis reçu » → accepter / refuser).
 */
public record DevisResponse(
    String id,
    String reservationId,
    BigDecimal montant,
    BigDecimal dureeEstimeeH,
    String conditions,
    StatutDevis statut,
    LocalDateTime emisLe
) {
    public static DevisResponse from(Devis d) {
        return new DevisResponse(
            d.getId(), d.getReservationId(), d.getMontant(), d.getDureeEstimeeH(),
            d.getConditions(), d.getStatut(), d.getEmisLe());
    }
}
