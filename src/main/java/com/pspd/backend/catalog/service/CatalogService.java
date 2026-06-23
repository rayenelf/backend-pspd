package com.pspd.backend.catalog.service;

import com.pspd.backend.catalog.domain.Categorie;
import com.pspd.backend.catalog.domain.StatutService;
import com.pspd.backend.catalog.dto.CategorieResponse;
import com.pspd.backend.catalog.dto.ServiceResponse;
import com.pspd.backend.catalog.repository.CategorieRepository;
import com.pspd.backend.catalog.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lecture publique du catalogue (B1) : arbre des catégories + services d'une catégorie.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategorieRepository categorieRepository;
    private final ServiceRepository   serviceRepository;

    /**
     * Reconstruit l'arbre des catégories actives en une seule requête.
     * Les catégories sont groupées par parent, puis assemblées récursivement
     * depuis les racines (parentId == null).
     */
    @Transactional(readOnly = true)
    public List<CategorieResponse> categoriesTree() {
        List<Categorie> actives = categorieRepository.findByActifTrueOrderByLibelleAsc();

        Map<String, List<Categorie>> parEnfantsDeParent = actives.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Categorie::getParentId));

        return actives.stream()
                .filter(c -> c.getParentId() == null)
                .map(racine -> toNode(racine, parEnfantsDeParent))
                .toList();
    }

    private CategorieResponse toNode(Categorie c, Map<String, List<Categorie>> enfantsParParent) {
        List<CategorieResponse> enfants = enfantsParParent
                .getOrDefault(c.getId(), List.of())
                .stream()
                .map(e -> toNode(e, enfantsParParent))
                .toList();
        return CategorieResponse.from(c, enfants);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> servicesOfCategorie(String categorieId) {
        return serviceRepository.findByCategorieIdAndActifTrueAndStatutOrderByLibelleAsc(
                        categorieId, StatutService.APPROUVE)
                .stream()
                .map(ServiceResponse::from)
                .toList();
    }
}
