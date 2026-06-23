package com.pspd.backend.search.dto;

import java.math.BigDecimal;

/**
 * Projection brute issue de la requête de recherche (un prestataire validé +
 * le prix mini de ses services correspondants). Convertie en {@link SearchResult}
 * par le service, qui ajoute les champs géo (distance/ETA) calculés à part.
 */
public record SearchRow(
    String prestataireId,
    String slug,
    String nomCommercial,
    String categoriePrincipale,
    BigDecimal note,
    boolean certifie,
    String langues,
    String zoneIntervention,
    int rayonKm,
    BigDecimal prixIndicatif
) {}
