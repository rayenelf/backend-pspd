package com.pspd.backend.booking.web;

import com.pspd.backend.booking.dto.ReservationImageResponse;
import com.pspd.backend.booking.service.ReservationImageService;
import com.pspd.backend.common.storage.FileStorageService;
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

/**
 * Images de travail d'une réservation AVEC_DEVIS.
 *
 * <ul>
 *   <li>{@code POST /api/reservations/{id}/images}              — le client joint une photo</li>
 *   <li>{@code GET  /api/reservations/{id}/images}              — client/prestataire : métadonnées</li>
 *   <li>{@code GET  /api/reservations/{id}/images/{imageId}/file} — flux binaire (consultation)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/reservations/{id}/images")
@RequiredArgsConstructor
public class ReservationImageController {

    private final ReservationImageService imageService;
    private final FileStorageService      fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public ReservationImageResponse ajouter(@PathVariable String id,
                                            @RequestParam("file") MultipartFile file,
                                            Authentication auth) {
        return imageService.ajouter(id, auth.getName(), file);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public List<ReservationImageResponse> lister(@PathVariable String id, Authentication auth) {
        return imageService.lister(id, auth.getName());
    }

    @GetMapping("/{imageId}/file")
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public ResponseEntity<Resource> fichier(@PathVariable String id,
                                            @PathVariable String imageId,
                                            Authentication auth) {
        String storedPath  = imageService.resolveImagePath(id, imageId, auth.getName());
        Resource resource  = fileStorageService.loadAsResource(storedPath);
        String contentType = fileStorageService.contentTypeOf(storedPath);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(resource);
    }
}
