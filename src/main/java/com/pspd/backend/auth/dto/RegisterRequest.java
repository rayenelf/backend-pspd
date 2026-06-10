package com.pspd.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String role;
    private String type;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String motDePasse;
    private String raisonSociale;
    private String matriculeFiscal;
    private String nomCommercial;
    private String categoriePrincipale;
}
