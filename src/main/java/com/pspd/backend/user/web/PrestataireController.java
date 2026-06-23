package com.pspd.backend.user.web;

import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.domain.TypeDocument;
import com.pspd.backend.user.dto.DocumentResponse;
import com.pspd.backend.user.dto.PrestataireProfileResponse;
import com.pspd.backend.user.dto.PhotoResponse;
import com.pspd.backend.user.dto.UpdatePrestataireRequest;
import com.pspd.backend.user.service.DocumentService;
import com.pspd.backend.user.service.PrestatairePhotoService;
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

    private final UserService             userService;
    private final DocumentService         documentService;
    private final PrestatairePhotoService photoService;
    private final FileStorageService      fileStorageService;

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

    // ── Photo de profil (avatar) ─────────────────────────────────────────────

    /** Définit/remplace la photo de profil. Renvoie l'URL publique de l'avatar. */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarResponse> setAvatar(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        String url = photoService.setAvatar(authentication.getName(), file);
        return ResponseEntity.ok(new AvatarResponse(url));
    }

    /** Supprime la photo de profil. */
    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) {
        photoService.deleteAvatar(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // ── Portfolio (photos de réalisations) ───────────────────────────────────

    /** Ajoute une photo au portfolio (max 12). */
    @PostMapping(value = "/me/portfolio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> addPortfolioPhoto(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        PhotoResponse photo = photoService.addPhoto(authentication.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(photo);
    }

    /** Liste les photos de portfolio du prestataire courant. */
    @GetMapping("/me/portfolio")
    public ResponseEntity<List<PhotoResponse>> myPortfolio(Authentication authentication) {
        return ResponseEntity.ok(photoService.listMine(authentication.getName()));
    }

    /** Supprime une photo du portfolio (vérifie la propriété). */
    @DeleteMapping("/me/portfolio/{id}")
    public ResponseEntity<Void> deletePortfolioPhoto(
            @PathVariable String id, Authentication authentication) {
        photoService.deletePhoto(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    /** URL publique de l'avatar renvoyée après upload. */
    public record AvatarResponse(String url) {}
}
