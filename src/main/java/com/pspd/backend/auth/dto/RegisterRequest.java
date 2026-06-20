package com.pspd.backend.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Données d'inscription (cahier des charges §4).
 *
 * Les champs toujours obligatoires sont validés ici par bean validation
 * (déclenchée par {@code @Valid} sur le contrôleur). Les champs conditionnels
 * (selon CLIENT/PRESTATAIRE et le type de compte) sont vérifiés dans
 * {@code AuthService.register} car ils dépendent du rôle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Le rôle est requis")
    private String role;

    /** Type de client (PARTICULIER / ENTREPRISE) — requis pour un CLIENT. */
    private String type;

    /** Type de prestataire (INDIVIDUEL / SOCIETE) — requis pour un PRESTATAIRE. */
    private String typePrestataire;

    @NotBlank(message = "Le nom est requis")
    private String nom;

    private String prenom;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le téléphone est requis")
    private String telephone;

    @NotBlank(message = "L'adresse est requise")
    private String adresse;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;

    // ── Champs entreprise / société ──
    private String raisonSociale;
    private String matriculeFiscal;

    // ── Champs prestataire ──
    private String nomCommercial;
    private String categoriePrincipale;
    private String zoneIntervention;

    /** Consentement aux CGU et à la politique de confidentialité (§22) — obligatoire. */
    @AssertTrue(message = "Vous devez accepter les CGU et la politique de confidentialité")
    private boolean cguAcceptees;
}
