package com.pspd.backend.user.domain;

/**
 * Typologie du prestataire (cahier des charges §3).
 *
 * <ul>
 *   <li>{@code INDIVIDUEL} — artisan, technicien ou freelance qui exerce seul.</li>
 *   <li>{@code SOCIETE}    — société prestataire qui gère plusieurs employés et
 *       répartit les missions. Son tableau de bord exposera, à terme, des
 *       fonctions supplémentaires (gestion des employés, affectation des
 *       missions) absentes du compte individuel.</li>
 * </ul>
 */
public enum TypePrestataire {
    INDIVIDUEL,
    SOCIETE
}
