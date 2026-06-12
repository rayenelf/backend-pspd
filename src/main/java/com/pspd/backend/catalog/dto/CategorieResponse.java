package com.pspd.backend.catalog.dto;

import com.pspd.backend.catalog.domain.Categorie;

import java.util.List;

/** Catégorie avec ses sous-catégories (arbre du catalogue public). */
public record CategorieResponse(
    String id,
    String libelle,
    String slug,
    List<CategorieResponse> enfants
) {
    public static CategorieResponse from(Categorie c, List<CategorieResponse> enfants) {
        return new CategorieResponse(c.getId(), c.getLibelle(), c.getSlug(), enfants);
    }
}
