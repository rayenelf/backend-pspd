package com.pspd.backend.booking.repository;

import com.pspd.backend.booking.domain.Devis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès aux devis (flow AVEC_DEVIS). Un seul devis par réservation
 * (contrainte {@code UNIQUE reservation_id}).
 */
public interface DevisRepository extends JpaRepository<Devis, String> {

    Optional<Devis> findByReservationId(String reservationId);

    boolean existsByReservationId(String reservationId);
}
