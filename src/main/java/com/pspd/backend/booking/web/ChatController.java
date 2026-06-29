package com.pspd.backend.booking.web;

import com.pspd.backend.booking.dto.ConversationResponse;
import com.pspd.backend.booking.dto.MessageResponse;
import com.pspd.backend.booking.dto.SendMessageRequest;
import com.pspd.backend.booking.service.ChatService;
import com.pspd.backend.common.storage.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat instantané lié à une réservation.
 *
 * <ul>
 *   <li>{@code GET  /api/reservations/{id}/conversation}             — obtient ou crée la conversation</li>
 *   <li>{@code GET  /api/conversations/{id}/messages}                — liste les messages (poll)</li>
 *   <li>{@code POST /api/conversations/{id}/messages}                — envoie un message texte</li>
 *   <li>{@code POST /api/conversations/{id}/messages/image}          — envoie une image</li>
 *   <li>{@code GET  /api/conversations/{id}/messages/{msgId}/image}  — sert le fichier image</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService        chatService;
    private final FileStorageService fileStorageService;

    @GetMapping("/api/reservations/{id}/conversation")
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public ConversationResponse getOrCreate(@PathVariable String id, Authentication auth) {
        return chatService.getOrCreate(id, auth.getName());
    }

    @GetMapping("/api/conversations/{id}/messages")
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public List<MessageResponse> messages(
            @PathVariable String id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            Authentication auth) {
        if (since != null) {
            return chatService.getMessagesSince(id, auth.getName(), since);
        }
        return chatService.getMessages(id, auth.getName());
    }

    @PostMapping("/api/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public MessageResponse sendMessage(@PathVariable String id,
                                       @Valid @RequestBody SendMessageRequest req,
                                       Authentication auth) {
        return chatService.sendMessage(id, auth.getName(), req.contenu());
    }

    @PostMapping(value = "/api/conversations/{id}/messages/image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public MessageResponse sendImage(@PathVariable String id,
                                     @RequestParam("file") MultipartFile file,
                                     Authentication auth) {
        return chatService.sendImage(id, auth.getName(), file);
    }

    @GetMapping("/api/conversations/{id}/messages/{msgId}/image")
    @PreAuthorize("hasAnyRole('CLIENT','PRESTATAIRE')")
    public ResponseEntity<Resource> getImage(@PathVariable String id,
                                             @PathVariable String msgId,
                                             Authentication auth) {
        String storedPath  = chatService.resolveMessageImage(id, msgId, auth.getName());
        Resource resource  = fileStorageService.loadAsResource(storedPath);
        String contentType = fileStorageService.contentTypeOf(storedPath);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(resource);
    }
}
