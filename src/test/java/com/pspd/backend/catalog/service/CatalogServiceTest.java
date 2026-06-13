package com.pspd.backend.catalog.service;

import com.pspd.backend.catalog.domain.Categorie;
import com.pspd.backend.catalog.domain.Service;
import com.pspd.backend.catalog.dto.CategorieResponse;
import com.pspd.backend.catalog.dto.ServiceResponse;
import com.pspd.backend.catalog.repository.CategorieRepository;
import com.pspd.backend.catalog.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock CategorieRepository categorieRepository;
    @Mock ServiceRepository   serviceRepository;
    @InjectMocks CatalogService service;

    private Categorie cat(String id, String parentId, String libelle) {
        Categorie c = new Categorie();
        c.setId(id); c.setParentId(parentId); c.setLibelle(libelle);
        c.setSlug(libelle.toLowerCase()); c.setActif(true);
        return c;
    }

    @Test
    void categoriesTree_assemble_l_arbre_par_parent() {
        Categorie racine = cat("r1", null, "Maison");
        Categorie enfant = cat("e1", "r1", "Plomberie");
        Categorie racine2 = cat("r2", null, "Jardinage");
        when(categorieRepository.findByActifTrueOrderByLibelleAsc())
                .thenReturn(List.of(racine, enfant, racine2));

        List<CategorieResponse> tree = service.categoriesTree();

        assertThat(tree).extracting(CategorieResponse::id).containsExactlyInAnyOrder("r1", "r2");
        CategorieResponse maison = tree.stream().filter(c -> c.id().equals("r1")).findFirst().orElseThrow();
        assertThat(maison.enfants()).extracting(CategorieResponse::id).containsExactly("e1");
    }

    @Test
    void servicesOfCategorie_mappe_les_services() {
        Service s = new Service();
        s.setId("s1"); s.setCategorieId("r1"); s.setLibelle("Réparation fuite");
        s.setPrixIndicatif(BigDecimal.valueOf(40)); s.setActif(true);
        when(serviceRepository.findByCategorieIdAndActifTrueOrderByLibelleAsc("r1"))
                .thenReturn(List.of(s));

        List<ServiceResponse> out = service.servicesOfCategorie("r1");

        assertThat(out).extracting(ServiceResponse::libelle).containsExactly("Réparation fuite");
    }
}
