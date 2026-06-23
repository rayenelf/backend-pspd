package com.pspd.backend.catalog.web;

import com.pspd.backend.catalog.dto.CreateServiceRequest;
import com.pspd.backend.catalog.dto.ServiceResponse;
import com.pspd.backend.catalog.dto.UpdateServiceRequest;
import com.pspd.backend.catalog.service.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Gestion des services du catalogue — entièrement réservé à l'admin (B5).
 * La lecture publique des services passe par {@code GET /api/categories/{id}/services}.
 */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServiceController {

    private final AdminCatalogService adminCatalogService;

    @PostMapping
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCatalogService.createService(req));
    }

    // ── Propositions de services (prestataires → validation admin) ────────────

    /** File d'attente des services proposés par des prestataires. */
    @GetMapping("/pending")
    public java.util.List<ServiceResponse> pending() {
        return adminCatalogService.listPendingServices();
    }

    /** Approuve une proposition : le service rejoint le catalogue public. */
    @PostMapping("/{id}/approve")
    public ServiceResponse approve(@PathVariable String id) {
        return adminCatalogService.approveService(id);
    }

    /** Rejette une proposition : suppression du service proposé. */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable String id) {
        adminCatalogService.rejectService(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ServiceResponse update(@PathVariable String id, @RequestBody UpdateServiceRequest req) {
        return adminCatalogService.updateService(id, req);
    }

    /** Désactivation logique (le service reste en base, masqué du catalogue). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        adminCatalogService.deactivateService(id);
        return ResponseEntity.noContent().build();
    }
}
