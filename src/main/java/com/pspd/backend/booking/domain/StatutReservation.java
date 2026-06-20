package com.pspd.backend.booking.domain;

/**
 * Cycle de vie d'une réservation (Epic C, voir {@code 12-state-reservation.png}).
 *
 * <p>Transitions autorisées en Sprint 3 :</p>
 * <pre>
 *   EN_ATTENTE → ACCEPTEE | REFUSEE | ANNULEE
 *   ACCEPTEE   → EN_COURS | ANNULEE
 *   EN_COURS   → TERMINEE
 * </pre>
 *
 * <p>{@link #EN_LITIGE} (Epic F) reste hors périmètre Sprint 3. La table des
 * transitions est implémentée par {@code ReservationStateService} (Dev B).</p>
 */
public enum StatutReservation {
    EN_ATTENTE,
    ACCEPTEE,
    REFUSEE,
    EN_COURS,
    TERMINEE,
    ANNULEE,
    EN_LITIGE
}
