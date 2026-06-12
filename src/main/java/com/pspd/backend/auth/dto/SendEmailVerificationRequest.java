package com.pspd.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailVerificationRequest(@NotBlank @Email String email) {
}