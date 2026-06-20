package com.pspd.backend.notification.repository;

import com.pspd.backend.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    /** Notifications d'un utilisateur, non lues en premier puis par date décroissante. */
    List<Notification> findByDestinataireIdOrderByLuAscEnvoyeeLeDesc(String destinataireId);
}
