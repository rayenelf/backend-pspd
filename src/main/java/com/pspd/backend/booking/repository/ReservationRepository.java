package com.pspd.backend.booking.repository;

import com.pspd.backend.booking.domain.Reservation;
import com.pspd.backend.booking.domain.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Accès aux réservations (Epic C) — couche partagée Dev A / Dev B.
 * Les finders « client » servent l'écran « Mes réservations » (Dev A),
 * les finders « prestataire » servent « Mes missions » (Dev B).
 */
public interface ReservationRepository extends JpaRepository<Reservation, String> {

    // ── Côté client (Dev A) ──────────────────────────────────────────────────
    List<Reservation> findByClientIdOrderByCreeLeDesc(String clientId);
    List<Reservation> findByClientIdAndStatutOrderByCreeLeDesc(String clientId, StatutReservation statut);

    // ── Côté prestataire (Dev B) ─────────────────────────────────────────────
    List<Reservation> findByPrestataireIdOrderByCreeLeDesc(String prestataireId);
    List<Reservation> findByPrestataireIdAndStatutOrderByCreeLeDesc(String prestataireId, StatutReservation statut);
}
