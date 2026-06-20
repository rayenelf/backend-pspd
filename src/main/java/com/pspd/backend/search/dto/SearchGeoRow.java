package com.pspd.backend.search.dto;

import java.math.BigDecimal;

/**
 * Projection pour la recherche géolocalisée (B4) : comme {@link SearchRow}
 * mais avec les coordonnées, afin de calculer la distance côté service.
 */
public record SearchGeoRow(
    String prestataireId,
    String nomCommercial,
    String categoriePrincipale,
    BigDecimal note,
    boolean certifie,
    String langues,
    String zoneIntervention,
    int rayonKm,
    BigDecimal prixIndicatif,
    Double latitude,
    Double longitude
) {}
