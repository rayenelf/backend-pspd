package com.pspd.backend.booking.repository;

import com.pspd.backend.booking.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByConversationIdOrderByEnvoyeLeAsc(String conversationId);
    List<Message> findByConversationIdAndEnvoyeLeGreaterThanEqualOrderByEnvoyeLeAsc(String conversationId, LocalDateTime since);
}
