package com.pspd.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePrestataireRequest(
        @NotBlank @Size(max = 180) String nomCommercial,
        @Size(max = 80)  String categoriePrincipale,
        @Size(max = 180) String zoneIntervention,
        Integer rayonKm,
        @Size(max = 120) String langues,
        Double latitude,
        Double longitude
) {}
