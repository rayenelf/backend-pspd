package com.pspd.backend.booking.service;

import com.pspd.backend.booking.domain.Conversation;
import com.pspd.backend.booking.domain.Message;
import com.pspd.backend.booking.dto.ConversationResponse;
import com.pspd.backend.booking.dto.MessageResponse;
import com.pspd.backend.booking.repository.ConversationRepository;
import com.pspd.backend.booking.repository.MessageRepository;
import com.pspd.backend.booking.repository.ReservationRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository      messageRepository;
    private final ReservationRepository  reservationRepository;
    private final UserRepository         userRepository;
    private final FileStorageService     fileStorageService;

    @Transactional
    public ConversationResponse getOrCreate(String reservationId, String userEmail) {
        verifierAcces(reservationId, userEmail);
        Conversation conv = conversationRepository.findByReservationId(reservationId)
            .orElseGet(() -> conversationRepository.save(
                Conversation.builder().reservationId(reservationId).build()
            ));
        return ConversationResponse.from(conv);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String conversationId, String userEmail) {
        Conversation conv = chargerConversation(conversationId);
        verifierAcces(conv.getReservationId(), userEmail);
        return enrichir(messageRepository.findByConversationIdOrderByEnvoyeLeAsc(conversationId));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesSince(String conversationId, String userEmail, LocalDateTime since) {
        Conversation conv = chargerConversation(conversationId);
        verifierAcces(conv.getReservationId(), userEmail);
        return enrichir(messageRepository
            .findByConversationIdAndEnvoyeLeGreaterThanEqualOrderByEnvoyeLeAsc(conversationId, since));
    }

    @Transactional
    public MessageResponse sendMessage(String conversationId, String userEmail, String contenu) {
        Conversation conv = chargerConversation(conversationId);
        verifierAcces(conv.getReservationId(), userEmail);
        User auteur = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));
        Message msg = messageRepository.save(Message.builder()
            .conversationId(conversationId)
            .auteurId(auteur.getId())
            .contenu(contenu)
            .build());
        return MessageResponse.from(msg, auteur.getPrenom() + " " + auteur.getNom());
    }

    @Transactional
    public MessageResponse sendImage(String conversationId, String userEmail, MultipartFile file) {
        Conversation conv = chargerConversation(conversationId);
        verifierAcces(conv.getReservationId(), userEmail);
        User auteur = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));
        String url = fileStorageService.store(file);
        Message msg = messageRepository.save(Message.builder()
            .conversationId(conversationId)
            .auteurId(auteur.getId())
            .contenu(file.getOriginalFilename())
            .pieceJointeUrl(url)
            .build());
        return MessageResponse.from(msg, auteur.getPrenom() + " " + auteur.getNom());
    }

    @Transactional(readOnly = true)
    public String resolveMessageImage(String conversationId, String messageId, String userEmail) {
        Conversation conv = chargerConversation(conversationId);
        verifierAcces(conv.getReservationId(), userEmail);
        Message msg = messageRepository.findById(messageId)
            .orElseThrow(() -> ApiException.notFound("Message introuvable."));
        if (!msg.getConversationId().equals(conversationId) || msg.getPieceJointeUrl() == null) {
            throw ApiException.notFound("Image introuvable.");
        }
        return msg.getPieceJointeUrl();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<MessageResponse> enrichir(List<Message> messages) {
        Set<String> ids = messages.stream().map(Message::getAuteurId).collect(Collectors.toSet());
        Map<String, String> noms = userRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(User::getId, u -> u.getPrenom() + " " + u.getNom()));
        return messages.stream()
            .map(m -> MessageResponse.from(m, noms.getOrDefault(m.getAuteurId(), "Inconnu")))
            .toList();
    }

    private Conversation chargerConversation(String conversationId) {
        return conversationRepository.findById(conversationId)
            .orElseThrow(() -> ApiException.notFound("Conversation introuvable."));
    }

    private void verifierAcces(String reservationId, String email) {
        var reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> ApiException.notFound("Réservation introuvable."));
        String userId = userRepository.findByEmail(email)
            .map(User::getId)
            .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));
        if (!userId.equals(reservation.getClientId()) && !userId.equals(reservation.getPrestataireId())) {
            throw ApiException.forbidden("Vous n'êtes pas concerné par cette réservation.");
        }
    }
}
