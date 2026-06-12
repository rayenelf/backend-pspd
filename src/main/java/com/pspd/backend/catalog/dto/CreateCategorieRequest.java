package com.pspd.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;

/** Création d'une catégorie (admin). {@code parentId} null = catégorie racine. */
public record CreateCategorieRequest(
    @NotBlank String libelle,
    @NotBlank String slug,
    String parentId
) {}
