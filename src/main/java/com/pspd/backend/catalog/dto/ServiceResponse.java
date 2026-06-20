package com.pspd.backend.catalog.dto;

import com.pspd.backend.catalog.domain.Service;

import java.math.BigDecimal;

public record ServiceResponse(
    String id,
    String categorieId,
    String libelle,
    String description,
    BigDecimal prixIndicatif,
    String unite
) {
    public static ServiceResponse from(Service s) {
        return new ServiceResponse(
            s.getId(), s.getCategorieId(), s.getLibelle(),
            s.getDescription(), s.getPrixIndicatif(), s.getUnite());
    }
}
