package com.pspd.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Création d'un service dans une catégorie (admin). */
public record CreateServiceRequest(
    @NotBlank String categorieId,
    @NotBlank String libelle,
    String description,
    BigDecimal prixIndicatif,
    String unite
) {}
