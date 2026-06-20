package com.pspd.backend.user.web;

import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.domain.TypeDocument;
import com.pspd.backend.user.dto.DocumentResponse;
import com.pspd.backend.user.dto.PrestataireProfileResponse;
import com.pspd.backend.user.dto.UpdatePrestataireRequest;
import com.pspd.backend.user.service.DocumentService;
import com.pspd.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/prestataires")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PRESTATAIRE')")   // #2 — réservé aux prestataires
public class PrestataireController {

    private final UserService        userService;
    private final DocumentService    documentService;
    private final FileStorageService fileStorageService;

    /** Profil professionnel du prestataire connecté (pré-remplissage + statut). */
    @GetMapping("/me")
    public PrestataireProfileResponse getMe(Authentication authentication) {
        return userService.getPrestataireProfile(authentication.getName());
    }

    /** Mise à jour du profil professionnel. */
    @PatchMapping("/me")
    public ResponseEntity<Void> updateMe(
            @Valid @RequestBody UpdatePrestataireRequest req,
            Authentication authentication) {
        userService.updatePrestataire(authentication.getName(), req);
        return ResponseEntity.noContent().build();
    }

    /** Dépôt d'un document légal (B9). Le document est créé en statut EN_ATTENTE. */
    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("type") TypeDocument type,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        DocumentResponse doc = documentService.upload(authentication.getName(), type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    /** Liste des documents déposés par le prestataire courant. */
    @GetMapping("/me/documents")
    public ResponseEntity<List<DocumentResponse>> myDocuments(Authentication authentication) {
        return ResponseEntity.ok(documentService.listMine(authentication.getName()));
    }

    /**
     * Consultation d'un document par son propre prestataire (affichage inline).
     * Vérifie que le document appartient bien au prestataire connecté.
     */
    @GetMapping("/me/documents/{id}/file")
    public ResponseEntity<Resource> myDocumentFile(
            @PathVariable String id, Authentication authentication) {
        String storedPath = documentService.resolveOwnedDocumentPath(authentication.getName(), id);
        Resource resource  = fileStorageService.loadAsResource(storedPath);
        String contentType = fileStorageService.contentTypeOf(storedPath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
