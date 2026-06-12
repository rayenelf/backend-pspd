package com.pspd.backend.catalog.dto;

import java.math.BigDecimal;

/** Mise à jour partielle d'un service (admin). Tous les champs sont optionnels. */
public record UpdateServiceRequest(
    String libelle,
    String description,
    BigDecimal prixIndicatif,
    String unite
) {}
