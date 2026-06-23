package com.pspd.backend.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Photo de réalisation du prestataire (portfolio public).
 * Contrairement aux {@link DocumentLegal} (privés, validés par l'admin), ces
 * photos sont publiques et destinées à l'affichage sur le profil prestataire.
 */
@Entity
@Table(name = "photos_travail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoTravail {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestataire_id", nullable = false)
    private Prestataire prestataire;

    @Column(name = "url_fichier", length = 400, nullable = false)
    private String urlFichier;

    /** Ordre d'affichage dans la galerie. */
    @Column(nullable = false)
    @Builder.Default
    private int ordre = 0;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @PrePersist
    void init() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.creeLe == null) this.creeLe = LocalDateTime.now();
    }
}
