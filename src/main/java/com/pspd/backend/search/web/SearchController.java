package com.pspd.backend.search.web;

import com.pspd.backend.common.dto.PageResponse;
import com.pspd.backend.search.dto.SearchResult;
import com.pspd.backend.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Recherche publique de prestataires (B2/B3). Tous les paramètres sont optionnels.
 *
 * Exemple : {@code GET /api/search?service=<id>&prixMax=200&noteMin=4&certifie=true&tri=mieuxNote&page=0&size=20}
 */
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/api/search")
    public PageResponse<SearchResult> search(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) BigDecimal prixMax,
            @RequestParam(required = false) BigDecimal noteMin,
            @RequestParam(required = false) Boolean certifie,
            @RequestParam(required = false) String langue,
            @RequestParam(required = false, defaultValue = "mieuxNote") String tri,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            // B4 — géolocalisation (optionnel). lat+lng → distance/ETA + tri proche/plusRapide.
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double rayon) {

        return searchService.search(service, prixMax, noteMin, certifie, langue, tri, page, size, lat, lng, rayon);
    }
}
