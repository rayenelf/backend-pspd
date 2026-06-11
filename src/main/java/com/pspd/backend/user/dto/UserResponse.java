package com.pspd.backend.user.dto;

import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.StatutCompte;
import com.pspd.backend.user.domain.User;

/**
 * DTO retourné par GET /api/users/me.
 * Utilisé également comme référence pour les claims du JWT
 * (prenom, nom, email, role, uid).
 */
public record UserResponse(
        String id,
        String email,
        String prenom,
        String nom,
        String telephone,
        Role role,
        StatutCompte statutCompte,
        boolean doubleAuthActive
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPrenom(),
                user.getNom(),
                user.getTelephone(),
                user.getRole(),
                user.getStatutCompte(),
                user.isDoubleAuthActive()
        );
    }
}
