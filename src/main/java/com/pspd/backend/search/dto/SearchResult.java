package com.pspd.backend.search.dto;

import java.math.BigDecimal;

/**
 * Résultat de recherche exposé au client (B2/B3). {@code distanceKm} et
 * {@code etaMin} restent null tant que la géolocalisation (B4 / migration V5
 * lat-lng) n'est pas activée.
 */
public record SearchResult(
    String prestataireId,
    String nomCommercial,
    String categoriePrincipale,
    BigDecimal note,
    boolean certifie,
    String langues,
    String zoneIntervention,
    int rayonKm,
    BigDecimal prixIndicatif,
    Double distanceKm,
    Integer etaMin
) {
    public static SearchResult from(SearchRow r) {
        return new SearchResult(
            r.prestataireId(), r.nomCommercial(), r.categoriePrincipale(),
            r.note(), r.certifie(), r.langues(), r.zoneIntervention(),
            r.rayonKm(), r.prixIndicatif(),
            null, null);
    }

    /** Variante géolocalisée (B4) : ajoute la distance (km) et l'ETA (min) calculés. */
    public static SearchResult withGeo(SearchGeoRow r, double distanceKm, int etaMin) {
        return new SearchResult(
            r.prestataireId(), r.nomCommercial(), r.categoriePrincipale(),
            r.note(), r.certifie(), r.langues(), r.zoneIntervention(),
            r.rayonKm(), r.prixIndicatif(),
            distanceKm, etaMin);
    }
}
