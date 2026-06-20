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

    @PrePersist
    void init() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
