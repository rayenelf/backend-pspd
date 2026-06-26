package com.pspd.backend.catalog.repository;

import com.pspd.backend.catalog.domain.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategorieRepository extends JpaRepository<Categorie, String> {

    Optional<Categorie> findByLibelleIgnoreCase(String libelle);

    /** Catégories actives, pour reconstruire l'arbre du catalogue public. */
    List<Categorie> findByActifTrueOrderByLibelleAsc();

    boolean existsBySlug(String slug);

    /** Unicité du slug à la mise à jour : ignore la catégorie elle-même (B5). */
    boolean existsBySlugAndIdNot(String slug, String id);

    /** Vrai s'il reste au moins une sous-catégorie active (blocage de désactivation, B5). */
    boolean existsByParentIdAndActifTrue(String parentId);
}
