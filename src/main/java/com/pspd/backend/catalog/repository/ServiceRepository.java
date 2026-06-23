package com.pspd.backend.catalog.repository;

import com.pspd.backend.catalog.domain.Service;
import com.pspd.backend.catalog.domain.StatutService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, String> {

    /** Services actifs ET approuvés d'une catégorie (catalogue public). */
    List<Service> findByCategorieIdAndActifTrueAndStatutOrderByLibelleAsc(
            String categorieId, StatutService statut);

    /** Vrai s'il reste au moins un service actif rattaché (blocage de désactivation catégorie, B5). */
    boolean existsByCategorieIdAndActifTrue(String categorieId);

    /** Tous les services approuvés et actifs (sélection par le prestataire). */
    List<Service> findByActifTrueAndStatutOrderByLibelleAsc(StatutService statut);

    /** Services en attente de validation (file d'attente admin). */
    List<Service> findByStatutOrderByLibelleAsc(StatutService statut);

    /** Délie un service de tous les prestataires (préalable au rejet/suppression). */
    @Modifying
    @Query(value = "DELETE FROM prestataire_services WHERE service_id = :serviceId", nativeQuery = true)
    void unlinkFromAllPrestataires(@Param("serviceId") String serviceId);
}
