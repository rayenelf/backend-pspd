package com.pspd.backend.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Catégorie du catalogue (Epic B). Arbre via {@code parentId} auto-référencé
 * (null = catégorie racine). Mappé en colonne simple plutôt qu'en relation JPA :
 * l'arbre est reconstruit côté service, ce qui évite le lazy-loading récursif.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categorie {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(length = 120, nullable = false)
    private String libelle;

    @Column(length = 120, nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @PrePersist
    void init() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
