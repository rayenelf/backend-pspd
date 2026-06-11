package com.pspd.backend.user.dto;

import com.pspd.backend.user.domain.DocumentLegal;

import java.time.LocalDateTime;

public record DocumentResponse(
    String id,
    String type,
    String urlFichier,
    String statut,
    LocalDateTime verifieLe
) {
    public static DocumentResponse from(DocumentLegal d) {
        return new DocumentResponse(
            d.getId(),
            d.getType().name(),
            d.getUrlFichier(),
            d.getStatut().name(),
            d.getVerifieLe()
        );
    }
}
