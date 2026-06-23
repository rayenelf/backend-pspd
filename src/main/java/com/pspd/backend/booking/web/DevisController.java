package com.pspd.backend.booking.web;

import com.pspd.backend.booking.dto.CreateDevisRequest;
import com.pspd.backend.booking.dto.DevisResponse;
import com.pspd.backend.booking.service.DevisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints du flow AVEC_DEVIS.
 *
 * <ul>
 *   <li>{@code POST   /api/reservations/{id}/devis}          — prestataire émet le devis</li>
 *   <li>{@code GET    /api/reservations/{id}/devis}          — client ou prestataire le consultent</li>
 *   <li>{@code PATCH  /api/reservations/{id}/devis/accepter} — client accepte → réservation ACCEPTEE</li>
 *   <li>{@code PATCH  /api/reservations/{id}/devis/refuser}  — client refuse</li>
 * </ul>
 *
 * <p>Le contrôle de propriété (bon prestataire / bon client) est fait dans le service ;
 * {@code @PreAuthorize} ne filtre ici que le rôle.</p>
 */
@RestController
@RequestMapping("/api/reservations/{id}/devis")
@RequiredArgsConstructor
public class DevisController {

    private final DevisService devisService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PRESTATAIRE')")
    public DevisResponse creer(@PathVariable String id,
                               @Valid @RequestBody CreateDevisRequest req,
                               Authentication auth) {
        return devisService.creerDevis(id, auth.getName(), req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public DevisResponse consulter(@PathVariable String id, Authentication auth) {
        return devisService.getDevis(id, auth.getName());
    }

    @PatchMapping("/accepter")
    @PreAuthorize("hasRole('CLIENT')")
    public DevisResponse accepter(@PathVariable String id, Authentication auth) {
        return devisService.accepterDevis(id, auth.getName());
    }

    @PatchMapping("/refuser")
    @PreAuthorize("hasRole('CLIENT')")
    public DevisResponse refuser(@PathVariable String id, Authentication auth) {
        return devisService.refuserDevis(id, auth.getName());
    }
}
