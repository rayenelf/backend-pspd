package com.pspd.backend.catalog.dto;

/**
 * Mise à jour d'une catégorie (admin, B5).
 * {@code libelle} et {@code slug} ne sont appliqués que s'ils sont fournis (non vides).
 * {@code parentId} <b>remplace</b> le parent (null = catégorie racine) : le front édite
 * toujours le triplet complet, le champ doit donc refléter le parent voulu.
 */
public record UpdateCategorieRequest(
    String libelle,
    String slug,
    String parentId
) {}
