package com.pspd.backend.catalog.repository;

import com.pspd.backend.catalog.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, String> {

    /** Services actifs d'une catégorie (catalogue public). */
    List<Service> findByCategorieIdAndActifTrueOrderByLibelleAsc(String categorieId);

    /** Vrai s'il reste au moins un service actif rattaché (blocage de désactivation catégorie, B5). */
    boolean existsByCategorieIdAndActifTrue(String categorieId);
}
