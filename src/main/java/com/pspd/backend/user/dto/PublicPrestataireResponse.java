package com.pspd.backend.user.dto;

import com.pspd.backend.catalog.dto.ServiceResponse;
import com.pspd.backend.user.domain.Prestataire;

import java.math.BigDecimal;
import java.util.List;

/**
 * Profil PUBLIC d'un prestataire (page détail visible des clients) :
 * informations + avatar + portfolio + services proposés.
 * Aucune donnée sensible (documents légaux, email, téléphone) n'est exposée.
 */
public record PublicPrestataireResponse(
        String id,
        String slug,
        String nomCommercial,
        String categoriePrincipale,
        String zoneIntervention,
        int rayonKm,
        String langues,
        BigDecimal note,
        boolean certifie,
        String avatarUrl,
        List<PhotoResponse> portfolio,
        List<ServiceResponse> services
) {
    public static PublicPrestataireResponse of(Prestataire p, List<PhotoResponse> portfolio) {
        List<ServiceResponse> services = p.getServices().stream()
                .filter(s -> s.isActif())
                .map(ServiceResponse::from)
                .toList();
        return new PublicPrestataireResponse(
                p.getUserId(),
                p.getSlug(),
                p.getNomCommercial(),
                p.getCategoriePrincipale(),
                p.getZoneIntervention(),
                p.getRayonKm(),
                p.getLangues(),
                p.getNoteMoyenne(),
                p.isCertifie(),
                p.getPhotoUrl() != null ? "/api/prestataires/" + p.getUserId() + "/avatar" : null,
                portfolio,
                services
        );
    }
}
