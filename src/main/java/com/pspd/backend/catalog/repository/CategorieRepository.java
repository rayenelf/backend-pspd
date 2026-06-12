package com.pspd.backend.catalog.repository;

import com.pspd.backend.catalog.domain.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategorieRepository extends JpaRepository<Categorie, String> {

    /** Catégories actives, pour reconstruire l'arbre du catalogue public. */
    List<Categorie> findByActifTrueOrderByLibelleAsc();

    boolean existsBySlug(String slug);
}
