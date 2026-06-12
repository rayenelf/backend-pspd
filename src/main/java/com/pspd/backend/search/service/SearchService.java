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

/**
 * Recherche de prestataires (B2/B3). Délègue le filtrage/tri/pagination au
 * repository ; convertit les lignes en {@link SearchResult} (champs géo à null
 * tant que B4 n'est pas activé).
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;

    @Transactional(readOnly = true)
    public PageResponse<SearchResult> search(
            String serviceId, BigDecimal prixMax, BigDecimal noteMin,
            Boolean certifie, String langue, String tri, int page, int size) {

        // Le tri est porté par la requête (CASE) → pageable sans Sort pour éviter
        // tout conflit d'ORDER BY.
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));

        Page<SearchResult> results = searchRepository
                .search(blankToNull(serviceId), prixMax, noteMin, certifie, blankToNull(langue), tri, pageable)
                .map(SearchResult::from);

        return PageResponse.from(results);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
