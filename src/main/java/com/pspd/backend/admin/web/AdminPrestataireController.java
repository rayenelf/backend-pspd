package com.pspd.backend.admin.web;

import com.pspd.backend.admin.dto.AdminPrestataireResponse;
import com.pspd.backend.admin.dto.PrestataireStatsResponse;
import com.pspd.backend.admin.dto.ValidationDecisionRequest;
import com.pspd.backend.admin.service.AdminPrestataireService;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.domain.DocumentLegal;
import com.pspd.backend.user.domain.StatutValidation;
import com.pspd.backend.user.repository.DocumentLegalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Back-office admin — validation des prestataires (B9).
 * Entièrement réservé au rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPrestataireController {

    private final AdminPrestataireService adminPrestataireService;
    private final DocumentLegalRepository documentLegalRepository;
    private final FileStorageService      fileStorageService;

    /** Liste des prestataires, filtrable par statut (défaut : tous). */
    @GetMapping("/prestataires")
    public List<AdminPrestataireResponse> list(@RequestParam(required = false) String statut) {
        return adminPrestataireService.list(parseStatut(statut));
    }

    /** Compteurs par statut (cartes du tableau de bord). */
    @GetMapping("/prestataires/stats")
    public PrestataireStatsResponse stats() {
        return adminPrestataireService.stats();
    }

    /** Décision de validation : valider, suspendre/refuser, remettre en attente. */
    @PatchMapping("/prestataires/{id}/validation")
    public AdminPrestataireResponse decide(
            @PathVariable String id,
            @Valid @RequestBody ValidationDecisionRequest req) {
        return adminPrestataireService.decide(id, req.statut(), req.motif());
    }

    /** Consultation d'un document légal (affichage inline dans le navigateur). */
    @GetMapping("/documents/{id}/file")
    public ResponseEntity<Resource> documentFile(@PathVariable String id) {
        DocumentLegal doc = documentLegalRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Document introuvable."));
        Resource resource = fileStorageService.loadAsResource(doc.getUrlFichier());
        String contentType = fileStorageService.contentTypeOf(doc.getUrlFichier());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    private StatutValidation parseStatut(String statut) {
        if (statut == null || statut.isBlank()) return null;
        try {
            return StatutValidation.valueOf(statut);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("STATUT_INVALIDE", "Statut de filtre invalide.");
        }
    }
}
