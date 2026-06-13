package com.pspd.backend.search.service;

import com.pspd.backend.common.dto.PageResponse;
import com.pspd.backend.search.dto.SearchGeoRow;
import com.pspd.backend.search.dto.SearchResult;
import com.pspd.backend.search.dto.SearchRow;
import com.pspd.backend.search.repository.SearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock SearchRepository searchRepository;
    @InjectMocks SearchService service;

    // Centre de recherche : Tunis
    private static final double LAT = 36.8065, LNG = 10.1815;

    private SearchGeoRow row(String id, double lat, double lng, double note, double prix) {
        return new SearchGeoRow(id, id, "Cat", BigDecimal.valueOf(note), false,
                "Arabe", "Tunis", 20, BigDecimal.valueOf(prix), lat, lng);
    }

    // ── Mode standard (sans géoloc) délègue au repository paginé ───────────────

    @Test
    void sans_geoloc_delegue_la_recherche_paginee() {
        SearchRow r = new SearchRow("p1", "ElecPro", "Élec", BigDecimal.valueOf(4.5),
                true, "Arabe", "Tunis", 20, BigDecimal.valueOf(60));
        when(searchRepository.search(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        PageResponse<SearchResult> res = service.search(
                null, null, null, null, null, "mieuxNote", 0, 20, null, null, null);

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).nomCommercial()).isEqualTo("ElecPro");
        assertThat(res.content().get(0).distanceKm()).isNull(); // pas de géo
    }

    // ── Mode géolocalisé (B4) ──────────────────────────────────────────────────

    @Test
    void geoloc_calcule_la_distance_et_filtre_par_rayon() {
        SearchGeoRow proche = row("A", LAT, LNG, 4.0, 50);            // ~0 km
        SearchGeoRow loin   = row("B", 37.6, 11.2, 5.0, 100);        // ~120 km
        when(searchRepository.searchGeo(any(), any(), any(), any(), any()))
                .thenReturn(List.of(proche, loin));

        // rayon 50 km → "loin" exclu
        PageResponse<SearchResult> res = service.search(
                null, null, null, null, null, "proche", 0, 20, LAT, LNG, 50.0);

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).prestataireId()).isEqualTo("A");
        assertThat(res.content().get(0).distanceKm()).isLessThan(1.0);
        assertThat(res.content().get(0).etaMin()).isNotNull();
    }

    @Test
    void geoloc_tri_proche_ordonne_par_distance() {
        SearchGeoRow a = row("A", LAT, LNG, 4.0, 90);                 // 0 km
        SearchGeoRow b = row("B", 36.878, 10.324, 5.0, 10);          // ~16 km
        when(searchRepository.searchGeo(any(), any(), any(), any(), any()))
                .thenReturn(List.of(b, a));

        PageResponse<SearchResult> res = service.search(
                null, null, null, null, null, "proche", 0, 20, LAT, LNG, 100.0);

        assertThat(res.content()).extracting(SearchResult::prestataireId)
                .containsExactly("A", "B"); // le plus proche d'abord
    }

    @Test
    void geoloc_tri_moinsCher_ordonne_par_prix() {
        SearchGeoRow a = row("A", LAT, LNG, 4.0, 90);
        SearchGeoRow b = row("B", 36.878, 10.324, 5.0, 10);
        when(searchRepository.searchGeo(any(), any(), any(), any(), any()))
                .thenReturn(List.of(a, b));

        PageResponse<SearchResult> res = service.search(
                null, null, null, null, null, "moinsCher", 0, 20, LAT, LNG, 100.0);

        assertThat(res.content()).extracting(SearchResult::prestataireId)
                .containsExactly("B", "A"); // le moins cher d'abord (10 < 90)
    }

    @Test
    void geoloc_tri_mieuxNote_ordonne_par_note() {
        SearchGeoRow a = row("A", LAT, LNG, 4.0, 90);
        SearchGeoRow b = row("B", 36.878, 10.324, 5.0, 10);
        when(searchRepository.searchGeo(any(), any(), any(), any(), any()))
                .thenReturn(List.of(a, b));

        PageResponse<SearchResult> res = service.search(
                null, null, null, null, null, "mieuxNote", 0, 20, LAT, LNG, 100.0);

        assertThat(res.content()).extracting(SearchResult::prestataireId)
                .containsExactly("B", "A"); // meilleure note d'abord (5.0 > 4.0)
    }
}
