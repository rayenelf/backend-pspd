package com.pspd.backend.catalog.domain;

/**
 * Statut de validation d'un service du catalogue.
 * APPROUVE   : service officiel, visible dans le catalogue public.
 * EN_ATTENTE : service proposé par un prestataire, en attente de validation admin.
 */
public enum StatutService {
    APPROUVE,
    EN_ATTENTE
}
