package com.pspd.backend.user.dto;

import com.pspd.backend.catalog.dto.ServiceResponse;

import java.util.List;

/**
 * Vue « Mes services » du prestataire connecté.
 * - {@code available}   : tous les services approuvés du catalogue (choix possibles).
 * - {@code selectedIds} : ids des services actuellement proposés par le prestataire.
 * - {@code pending}     : propositions du prestataire en attente de validation admin.
 */
public record MesServicesResponse(
        List<ServiceResponse> available,
        List<String> selectedIds,
        List<ServiceResponse> pending
) {}
