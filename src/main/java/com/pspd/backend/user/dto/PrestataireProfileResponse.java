package com.pspd.backend.user.dto;

import com.pspd.backend.user.domain.Prestataire;

import java.math.BigDecimal;

/**
 * Profil professionnel du prestataire connecté (GET /api/prestataires/me).
 * Sert à pré-remplir le formulaire de profil et à connaître le statut de
 * validation (utilisé par le bandeau « documents manquants » côté front).
 */
public record PrestataireProfileResponse(
        String nomCommercial,
        String categoriePrincipale,
        String typePrestataire,
        String zoneIntervention,
        int rayonKm,
        String langues,
        String statutValidation,
        boolean certifie,
        BigDecimal noteMoyenne,
        int nombreDocuments,
        String avatarUrl
) {
    public static PrestataireProfileResponse of(Prestataire p, int nombreDocuments) {
        return new PrestataireProfileResponse(
                p.getNomCommercial(),
                p.getCategoriePrincipale(),
                p.getTypePrestataire() != null ? p.getTypePrestataire().name() : null,
                p.getZoneIntervention(),
                p.getRayonKm(),
                p.getLangues(),
                p.getStatutValidation().name(),
                p.isCertifie(),
                p.getNoteMoyenne(),
                nombreDocuments,
                // URL publique de l'avatar (null si pas de photo) — le front affiche
                // un placeholder dans ce cas.
                p.getPhotoUrl() != null ? "/api/prestataires/" + p.getUserId() + "/avatar" : null
        );
    }
}
