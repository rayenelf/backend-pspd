package com.pspd.backend.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Création d'une réservation immédiate (US C1, client).
 * Le {@code clientId} n'est PAS dans le corps : il est déduit du token JWT.
 * Le type est forcé à {@code IMMEDIATE} en Sprint 3 (le devis arrive en Phase 2).
 */
public record CreateReservationRequest(
    @NotBlank String prestataireId,
    @NotBlank String serviceId,
    String adresseId,
    @NotNull @Future LocalDate dateService,
    @NotNull LocalTime heureService,
    String description
) {}
