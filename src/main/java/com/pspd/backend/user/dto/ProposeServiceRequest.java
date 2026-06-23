package com.pspd.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Proposition d'un nouveau service par un prestataire (soumis à validation admin). */
public record ProposeServiceRequest(
        @NotBlank String categorieId,
        @NotBlank @Size(max = 160) String libelle,
        @Size(max = 2000) String description
) {}
