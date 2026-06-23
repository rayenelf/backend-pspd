package com.pspd.backend.booking.repository;

import com.pspd.backend.booking.domain.ReservationImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Accès aux images de travail jointes à une réservation AVEC_DEVIS.
 */
public interface ReservationImageRepository extends JpaRepository<ReservationImage, String> {

    List<ReservationImage> findByReservationIdOrderByOrdreAsc(String reservationId);

    long countByReservationId(String reservationId);
}
