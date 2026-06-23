package com.pspd.backend.user.web;

import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.dto.PhotoResponse;
import com.pspd.backend.user.dto.PublicPrestataireResponse;
import com.pspd.backend.user.service.PrestatairePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Lecture PUBLIQUE des photos du prestataire (avatar + portfolio), affichées
 * sur le profil public visible des clients. Aucune authentification requise
 * (cf. SecurityConfig). La gestion (ajout/suppression) reste dans
 * {@link PrestataireController}, réservée au prestataire propriétaire.
 */
@RestController
@RequestMapping("/api/prestataires")
@RequiredArgsConstructor
public class PublicPrestatairePhotoController {

    private final PrestatairePhotoService photoService;
    private final FileStorageService      fileStorageService;

    /** Profil public complet (infos + avatar + portfolio) — page détail client. */
    @GetMapping("/{id}/public")
    public PublicPrestataireResponse publicProfile(@PathVariable String id) {
        return photoService.publicProfile(id);
    }

    /** Photo de profil (avatar) d'un prestataire. */
    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> avatar(@PathVariable String id) {
        return streamImage(photoService.resolveAvatarPath(id));
    }

    /** Liste des photos de portfolio d'un prestataire. */
    @GetMapping("/{id}/portfolio")
    public List<PhotoResponse> portfolio(@PathVariable String id) {
        return photoService.listFor(id);
    }

    /** Fichier d'une photo de portfolio. */
    @GetMapping("/photos/{photoId}/file")
    public ResponseEntity<Resource> photoFile(@PathVariable String photoId) {
        return streamImage(photoService.resolvePhotoPath(photoId));
    }

    private ResponseEntity<Resource> streamImage(String storedPath) {
        Resource resource  = fileStorageService.loadAsResource(storedPath);
        String contentType = fileStorageService.contentTypeOf(storedPath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(resource);
    }
}
