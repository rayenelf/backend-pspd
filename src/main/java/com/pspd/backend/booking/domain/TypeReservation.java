package com.pspd.backend.booking.domain;

/**
 * Type de réservation (Epic C, voir {@code 02-modele-de-donnees §enums}).
 * En Sprint 3, seul {@link #IMMEDIATE} est utilisé ; {@link #AVEC_DEVIS}
 * (flux devis C3/C4/C5) est prévu en Phase 2.
 */
public enum TypeReservation {
    IMMEDIATE,
    AVEC_DEVIS
}
