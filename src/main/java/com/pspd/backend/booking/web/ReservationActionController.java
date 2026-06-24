package com.pspd.backend.booking.web;

import com.pspd.backend.booking.domain.StatutReservation;
import com.pspd.backend.booking.dto.AgendaEntryResponse;
import com.pspd.backend.booking.dto.ReservationResponse;
import com.pspd.backend.booking.service.ReservationWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transitions de cycle de vie d'une réservation (US C2, Dev B).
 * Contrôleur distinct de {@code ReservationController} (création/lecture, Dev A) pour éviter
 * tout conflit ; tous les chemins restent sous {@code /api/reservations/{id}/...}.
 *
 * <p>Le contrôle de propriété (bon prestataire / bon client) est fait dans le service ;
 * {@code @PreAuthorize} ne filtre ici que le rôle.</p>
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationActionController {

    private final ReservationWorkflowService workflow;

    /** Calendrier mensuel du prestataire : missions ACCEPTEE/EN_COURS pour le mois donné. */
    @GetMapping("/agenda")
    @PreAuthorize("hasRole('PRESTATAIRE')")
    public List<AgendaEntryResponse> agenda(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication auth) {
        java.time.LocalDate now = java.time.LocalDate.now();
        return workflow.agendaForMonth(auth.getName(),
            year  != null ? year  : now.getYear(),
            month != null ? month : now.getMonthValue());
    }

    /** Liste les missions du prestataire connecté (filtrable par statut). */
    @GetMapping("/mes-missions")
    @PreAuthorize("hasRole('PRESTATAIRE')")
    public List<ReservationResponse> mesMissions(
            @RequestParam(required = false) StatutReservation statut,
            Authentication auth) {
        return workflow.mesMissions(auth.getName(), statut);
    }

    @PatchMapping("/{id}/accepter")
    @PreAuthorize("hasRole('PRESTATAIRE')")
    public ReservationResponse accepter(@PathVariable String id, Authentication auth) {
        return workflow.accepter(id, auth.getName());
    }

    @PatchMapping("/{id}/refuser")
    @PreAuthorize("hasRole('PRESTATAIRE')")
    public ReservationResponse refuser(@PathVariable String id, Authentication auth) {
        return workflow.refuser(id, auth.getName());
    }

    @PatchMapping("/{id}/demarrer")
    @PreAuthorize("hasRole('PRESTATAIRE')")
    public ReservationResponse demarrer(@PathVariable String id, Authentication auth) {
        return workflow.demarrer(id, auth.getName());
    }

    @PatchMapping("/{id}/terminer")
    @PreAuthorize("hasRole('PRESTATAIRE')")
    public ReservationResponse terminer(@PathVariable String id, Authentication auth) {
        return workflow.terminer(id, auth.getName());
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasRole('CLIENT')")
    public ReservationResponse annuler(@PathVariable String id, Authentication auth) {
        return workflow.annuler(id, auth.getName());
    }
}
