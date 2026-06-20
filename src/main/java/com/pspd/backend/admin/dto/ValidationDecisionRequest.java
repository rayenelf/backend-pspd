package com.pspd.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Décision de validation d'un prestataire (B9).
 * {@code statut} ∈ {VALIDE, SUSPENDU, VERIFICATION, EN_ATTENTE}.
 * {@code motif} recommandé en cas de refus (transmis au prestataire par email).
 */
public record ValidationDecisionRequest(
    @NotBlank String statut,
    String motif
) {}
