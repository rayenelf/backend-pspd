package com.pspd.backend.booking.dto;

import com.pspd.backend.booking.domain.Message;

import java.time.LocalDateTime;

public record MessageResponse(
    String id,
    String conversationId,
    String auteurId,
    String auteurNom,
    String contenu,
    String pieceJointeUrl,
    boolean lu,
    LocalDateTime envoyeLe
) {
    public static MessageResponse from(Message m, String auteurNom) {
        return new MessageResponse(
            m.getId(),
            m.getConversationId(),
            m.getAuteurId(),
            auteurNom,
            m.getContenu(),
            m.getPieceJointeUrl(),
            m.isLu(),
            m.getEnvoyeLe()
        );
    }
}
