package com.pspd.backend.booking.domain;

/**
 * Cycle de vie d'un devis (flow {@code AVEC_DEVIS}).
 *
 * <pre>
 *   ENVOYE      → ACCEPTE | REFUSE   (décision du client)
 *   NEGOCIATION → réservé Phase 2 (contre-proposition / messagerie)
 * </pre>
 *
 * <p>Distinct de {@link StatutReservation} : tant que le devis est {@code ENVOYE},
 * la réservation reste {@code EN_ATTENTE}. À l'acceptation, la réservation passe
 * {@code ACCEPTEE} et son {@code prixConvenu} prend le montant du devis.</p>
 */
public enum StatutDevis {
    ENVOYE,
    ACCEPTE,
    REFUSE,
    NEGOCIATION
}
