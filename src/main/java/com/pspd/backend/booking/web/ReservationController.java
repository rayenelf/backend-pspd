package com.pspd.backend.booking.web;

import com.pspd.backend.booking.domain.StatutReservation;
import com.pspd.backend.booking.dto.CreateReservationRequest;
import com.pspd.backend.booking.dto.ReservationResponse;
import com.pspd.backend.booking.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Création et lecture des réservations côté client (US C1, Dev A).
 * Les transitions (accepter/refuser/…) sont dans {@link ReservationActionController}.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** Crée une réservation immédiate. Réservé aux CLIENT. */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> creer(
            @Valid @RequestBody CreateReservationRequest req,
            Authentication auth) {
        ReservationResponse resp = reservationService.creer(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /** Liste les réservations du client connecté, filtrables par statut. */
    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public List<ReservationResponse> lister(
            @RequestParam(required = false) StatutReservation statut,
            Authentication auth) {
        return reservationService.listerPourClient(auth.getName(), statut);
    }

    /** Détail d'une réservation (accessible par le client ou le prestataire concerné). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public ReservationResponse getById(@PathVariable String id, Authentication auth) {
        return reservationService.getById(id, auth.getName());
    }
}
