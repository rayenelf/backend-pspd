package com.pspd.backend.booking.repository;

import com.pspd.backend.booking.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Optional<Conversation> findByReservationId(String reservationId);
}
