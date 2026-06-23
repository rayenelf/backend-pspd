package com.pspd.backend.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Corps de la requête de création d'un devis par le prestataire
 * (POST /api/reservations/{id}/devis).
 */
public record CreateDevisRequest(
    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être positif.")
    BigDecimal montant,

    @Positive(message = "La durée estimée doit être positive.")
    BigDecimal dureeEstimeeH,

    @Size(max = 2000, message = "Les conditions sont limitées à 2000 caractères.")
    String conditions
) {}
