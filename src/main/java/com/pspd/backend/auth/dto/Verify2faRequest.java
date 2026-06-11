package com.pspd.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Verify2faRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 6, message = "Le code doit contenir exactement 6 chiffres") String code
) {}
