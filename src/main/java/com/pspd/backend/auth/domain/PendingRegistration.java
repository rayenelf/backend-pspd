package com.pspd.backend.auth.domain;

import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.TypeClient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_registrations")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    // Champs spécifiques CLIENT
    @Column(name = "type_client")
    @Enumerated(EnumType.STRING)
    private TypeClient type;

    @Column(name = "raison_sociale")
    private String raisonSociale;

    @Column(name = "matricule_fiscal")
    private String matriculeFiscal;

    // Champs spécifiques PRESTATAIRE
    @Column(name = "nom_commercial")
    private String nomCommercial;

    @Column(name = "categorie_principale")
    private String categoriePrincipale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    void init() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusHours(24); // Expire après 24h
    }
}