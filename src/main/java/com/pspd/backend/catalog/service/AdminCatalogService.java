package com.pspd.backend.catalog.service;

import com.pspd.backend.catalog.domain.Categorie;
import com.pspd.backend.catalog.domain.Service;
import com.pspd.backend.catalog.dto.CreateCategorieRequest;
import com.pspd.backend.catalog.dto.CreateServiceRequest;
import com.pspd.backend.catalog.dto.UpdateServiceRequest;
import com.pspd.backend.catalog.dto.CategorieResponse;
import com.pspd.backend.catalog.dto.ServiceResponse;
import com.pspd.backend.catalog.repository.CategorieRepository;
import com.pspd.backend.catalog.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestion du catalogue réservée à l'admin (B5) : création de catégories/services,
 * mise à jour et désactivation logique d'un service.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final CategorieRepository categorieRepository;
    private final ServiceRepository   serviceRepository;

    @Transactional
    public CategorieResponse createCategorie(CreateCategorieRequest req) {
        if (categorieRepository.existsBySlug(req.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug déjà utilisé");
        }
        if (req.parentId() != null && !categorieRepository.existsById(req.parentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie parente introuvable");
        }
        Categorie c = categorieRepository.save(Categorie.builder()
                .libelle(req.libelle())
                .slug(req.slug())
                .parentId(req.parentId())
                .build());
        return CategorieResponse.from(c, java.util.List.of());
    }

    @Transactional
    public ServiceResponse createService(CreateServiceRequest req) {
        if (!categorieRepository.existsById(req.categorieId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie introuvable");
        }
        Service s = serviceRepository.save(Service.builder()
                .categorieId(req.categorieId())
                .libelle(req.libelle())
                .description(req.description())
                .prixIndicatif(req.prixIndicatif())
                .unite(req.unite())
                .build());
        return ServiceResponse.from(s);
    }

    @Transactional
    public ServiceResponse updateService(String id, UpdateServiceRequest req) {
        Service s = serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));
        if (req.libelle()       != null) s.setLibelle(req.libelle());
        if (req.description()   != null) s.setDescription(req.description());
        if (req.prixIndicatif() != null) s.setPrixIndicatif(req.prixIndicatif());
        if (req.unite()         != null) s.setUnite(req.unite());
        return ServiceResponse.from(serviceRepository.save(s));
    }

    /** Désactivation logique : le service disparaît du catalogue public sans suppression physique. */
    @Transactional
    public void deactivateService(String id) {
        Service s = serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));
        s.setActif(false);
        serviceRepository.save(s);
    }
}
