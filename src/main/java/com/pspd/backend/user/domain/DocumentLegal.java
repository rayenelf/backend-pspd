package com.pspd.backend.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents_legaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentLegal {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestataire_id", nullable = false)
    private Prestataire prestataire;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TypeDocument type;

    @Column(name = "url_fichier", length = 400, nullable = false)
    private String urlFichier;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private StatutValidation statut = StatutValidation.EN_ATTENTE;

    @Column(name = "verifie_le")
    private LocalDateTime verifieLe;

    @PrePersist
    void init() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
