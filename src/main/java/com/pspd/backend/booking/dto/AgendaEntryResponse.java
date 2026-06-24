package com.pspd.backend.booking.dto;

import com.pspd.backend.booking.domain.StatutReservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Entrée enrichie du calendrier prestataire : une mission acceptée ou en cours. */
public record AgendaEntryResponse(
    String id,
    LocalDate dateService,
    LocalTime heureService,
    StatutReservation statut,
    String clientNomComplet,
    String serviceLibelle,
    BigDecimal prixConvenu
) {}
