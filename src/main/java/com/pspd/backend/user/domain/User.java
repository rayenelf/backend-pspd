package com.pspd.backend.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 190, nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe_hash", length = 255)
    private String motDePasseHash;

    @Column(length = 30, nullable = false)
    private String telephone;

    @Column(length = 120)
    private String nom;

    @Column(length = 120)
    private String prenom;

    /** Adresse postale saisie à l'inscription (cahier des charges §4). */
    @Column(length = 255)
    private String adresse;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_compte", length = 20, nullable = false)
    @Builder.Default
    private StatutCompte statutCompte = StatutCompte.ACTIF;

    @Column(name = "double_auth_active", nullable = false)
    @Builder.Default
    private boolean doubleAuthActive = false;

    @Column(name = "email_verifie", nullable = false)
    @Builder.Default
    private boolean emailVerifie = false;

    /** Consentement aux CGU / politique de confidentialité (cahier des charges §22). */
    @Column(name = "cgu_acceptees", nullable = false)
    @Builder.Default
    private boolean cguAcceptees = false;

    /** Horodatage du consentement légal (preuve RGPD). */
    @Column(name = "consentement_le")
    private LocalDateTime consentementLe;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    void init() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.creeLe == null) this.creeLe = LocalDateTime.now();
    }
}
