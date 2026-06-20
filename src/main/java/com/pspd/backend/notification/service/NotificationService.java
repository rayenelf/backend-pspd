package com.pspd.backend.notification.service;

import com.pspd.backend.notification.domain.Notification;
import com.pspd.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Émission de notifications in-app (version minimale Sprint 3).
 * Persiste une ligne {@code notifications} et journalise l'envoi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** Notifie un utilisateur (titre + contenu). Canal PUSH (in-app) en Sprint 3. */
    public void notifier(String destinataireId, String titre, String contenu) {
        Notification notif = Notification.builder()
            .destinataireId(destinataireId)
            .canal("PUSH")
            .titre(titre)
            .contenu(contenu)
            .build();
        notificationRepository.save(notif);
        log.info("Notification → user={} : {}", destinataireId, titre);
    }
}
