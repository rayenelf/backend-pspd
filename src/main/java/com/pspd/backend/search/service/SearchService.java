package com.pspd.backend.search.service;

import com.pspd.backend.common.dto.PageResponse;
import com.pspd.backend.search.dto.SearchResult;
import com.pspd.backend.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Recherche de prestataires (B2/B3/B4).
 *
 * <ul>
 *   <li>Sans coordonnées (lat/lng absents) : filtrage + tri + pagination en base
 *       (mieuxNote / moinsCher).</li>
 *   <li>Avec coordonnées : on récupère les prestataires géolocalisés filtrés, puis
 *       on calcule la distance (Haversine), on filtre par rayon, on trie
 *       (proche / plusRapide / moinsCher / mieuxNote) et on pagine en mémoire.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double AVG_SPEED_KMH    = 30.0;   // pour l'ETA en ville
    private static final double DEFAULT_RAYON_KM = 50.0;

    private final SearchRepository searchRepository;

    @Transactional(readOnly = true)
    public PageResponse<SearchResult> search(
            String serviceId, String categoryId, BigDecimal prixMax, BigDecimal noteMin,
            Boolean certifie, String langue, String tri, int page, int size,
            Double lat, Double lng, Double rayon) {

        serviceId  = blankToNull(serviceId);
        categoryId = blankToNull(categoryId);
        langue     = blankToNull(langue);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        // ── Mode géolocalisé (B4) ────────────────────────────────────────────
        if (lat != null && lng != null) {
            double maxRayon = (rayon != null && rayon > 0) ? rayon : DEFAULT_RAYON_KM;
            final double flat = lat, flng = lng;

            List<SearchResult> all = searchRepository
                    .searchGeo(serviceId, categoryId, prixMax, noteMin, certifie, langue).stream()
                    .map(r -> {
                        double d = haversineKm(flat, flng, r.latitude(), r.longitude());
                        int eta = (int) Math.round(d / AVG_SPEED_KMH * 60);
                        return SearchResult.withGeo(r, round1(d), eta);
                    })
                    .filter(res -> res.distanceKm() <= maxRayon)
                    .sorted(geoComparator(tri))
                    .toList();

            int from = Math.min(safePage * safeSize, all.size());
            int to   = Math.min(from + safeSize, all.size());
            return PageResponse.of(all.subList(from, to), safePage, safeSize, all.size());
        }

        // ── Mode standard (B2/B3) ────────────────────────────────────────────
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<SearchResult> results = searchRepository
                .search(serviceId, categoryId, prixMax, noteMin, certifie, langue, tri, pageable)
                .map(SearchResult::from);
        return PageResponse.from(results);
    }

    private Comparator<SearchResult> geoComparator(String tri) {
        return switch (tri == null ? "" : tri) {
            case "proche", "plusRapide" -> Comparator.comparingDouble(SearchResult::distanceKm);
            case "moinsCher" -> Comparator.comparing(SearchResult::prixIndicatif,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(SearchResult::note,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    /** Distance Haversine entre deux points GPS, en kilomètres. */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
