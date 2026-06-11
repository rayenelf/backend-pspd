package com.pspd.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 120) String prenom,
        @Size(max = 120) String nom,
        @Pattern(regexp = "^\\+?[0-9 ]{6,30}$", message = "Numéro de téléphone invalide")
        String telephone
) {}
