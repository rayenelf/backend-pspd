package com.pspd.backend.admin.dto;

/** Compteurs de prestataires par statut (cartes du back-office, B9). */
public record PrestataireStatsResponse(
    long enAttente,
    long enVerification,
    long valides,
    long suspendus,
    long total
) {}
