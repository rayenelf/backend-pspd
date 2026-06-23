package com.pspd.backend.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service proposé dans une catégorie (Epic B). {@code prixIndicatif} est une
 * estimation affichée au client ; le prix réel est fixé à la réservation/devis.
 * {@code actif = false} → retiré du catalogue (désactivation logique, V5).
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "categorie_id", length = 36, nullable = false)
    private String categorieId;

    @Column(length = 160, nullable = false)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "prix_indicatif", precision = 12, scale = 2)
    private BigDecimal prixIndicatif;

    @Column(length = 40)
    private String unite;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    /** APPROUVE (catalogue public) ou EN_ATTENTE (proposé par un prestataire). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private StatutService statut = StatutService.APPROUVE;

    /** user_id du prestataire ayant proposé ce service (null pour le catalogue officiel). */
    @Column(name = "propose_par", length = 36)
    private String proposePar;

    @PrePersist
    void init() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
