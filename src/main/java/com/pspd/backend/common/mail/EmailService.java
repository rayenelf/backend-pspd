package com.pspd.backend.common.mail;

/** Envoi d'emails transactionnels (vérification, notifications sécurité). */
public interface EmailService {
    void send(String to, String subject, String body);
}
