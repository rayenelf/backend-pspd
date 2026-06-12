package com.pspd.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** currentPassword optionnel (compte OAuth sans mot de passe). */
public record ChangePasswordRequest(
    String currentPassword,
    @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String newPassword
) {}
