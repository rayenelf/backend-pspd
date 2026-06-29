package com.pspd.backend.booking.dto;

import com.pspd.backend.booking.domain.Conversation;

import java.time.LocalDateTime;

public record ConversationResponse(
    String id,
    String reservationId,
    LocalDateTime creeLe
) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getReservationId(), c.getCreeLe());
    }
}
