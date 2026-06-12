package com.pspd.backend.user.domain;

import com.pspd.backend.catalog.domain.Service;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "prestataires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestataire {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nom_commercial", length = 180, nullable = false)
    private String nomCommercial;

    @Column(name = "categorie_principale", length = 80)
    private String categoriePrincipale;

    @Column(name = "zone_intervention", length = 180)
    private String zoneIntervention;

    @Column(name = "rayon_km", nullable = false)
    @Builder.Default
    private int rayonKm = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", length = 20, nullable = false)
    @Builder.Default
    private StatutValidation statutValidation = StatutValidation.EN_ATTENTE;

    @Column(nullable = false)
    @Builder.Default
    private boolean certifie = false;

    @Column(name = "note_moyenne", precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal noteMoyenne = BigDecimal.ZERO;

    @Column(length = 120)
    private String langues;

    /** Services proposés par le prestataire (table de liaison N-N — Epic B). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "prestataire_services",
        joinColumns        = @JoinColumn(name = "prestataire_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id"))
    @Builder.Default
    private Set<Service> services = new HashSet<>();
}
