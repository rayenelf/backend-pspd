package com.pspd.backend.admin.dto;

import com.pspd.backend.user.domain.DocumentLegal;
import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.dto.DocumentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vue admin d'un prestataire pour la validation (B9) : infos compte + profil pro
 * + documents légaux déposés.
 */
public record AdminPrestataireResponse(
    String userId,
    String email,
    String prenom,
    String nom,
    String telephone,
    String nomCommercial,
    String categoriePrincipale,
    String zoneIntervention,
    String statutValidation,
    boolean certifie,
    BigDecimal noteMoyenne,
    LocalDateTime inscritLe,
    List<DocumentResponse> documents
) {
    public static AdminPrestataireResponse of(Prestataire p, List<DocumentLegal> docs) {
        var user = p.getUser();
        return new AdminPrestataireResponse(
            p.getUserId(),
            user.getEmail(),
            user.getPrenom(),
            user.getNom(),
            user.getTelephone(),
            p.getNomCommercial(),
            p.getCategoriePrincipale(),
            p.getZoneIntervention(),
            p.getStatutValidation().name(),
            p.isCertifie(),
            p.getNoteMoyenne(),
            user.getCreeLe(),
            docs.stream().map(DocumentResponse::from).toList()
        );
    }
}
