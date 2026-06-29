package com.pspd.backend.booking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "conversation_id", length = 36, nullable = false)
    private String conversationId;

    @Column(name = "auteur_id", length = 36, nullable = false)
    private String auteurId;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "piece_jointe_url", length = 400)
    private String pieceJointeUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean lu = false;

    @Column(name = "envoye_le", nullable = false, updatable = false)
    private LocalDateTime envoyeLe;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.envoyeLe == null) this.envoyeLe = LocalDateTime.now();
    }
}
