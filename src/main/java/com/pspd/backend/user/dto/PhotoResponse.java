package com.pspd.backend.user.dto;

import com.pspd.backend.user.domain.PhotoTravail;

/**
 * Photo de portfolio exposée au front. {@code url} pointe vers l'endpoint public
 * de service de fichier (pas le chemin disque brut), affichable directement.
 */
public record PhotoResponse(
    String id,
    String url
) {
    public static PhotoResponse from(PhotoTravail p) {
        return new PhotoResponse(p.getId(), "/api/prestataires/photos/" + p.getId() + "/file");
    }
}
