package com.pspd.backend.user.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TypeClient type;

    @Column(name = "raison_sociale", length = 180)
    private String raisonSociale;

    @Column(name = "matricule_fiscal", length = 60)
    private String matriculeFiscal;
}
