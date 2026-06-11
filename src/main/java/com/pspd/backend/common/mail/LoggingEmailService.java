package com.pspd.backend.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implémentation par défaut (dev, gratuit) : loggue l'email au lieu de l'envoyer.
 * Active tant que {@code app.mail.smtp.enabled} != true.
 * Permet de récupérer les liens de vérification / codes dans la console.
 */
@Service
@ConditionalOnProperty(name = "app.mail.smtp.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class LoggingEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                ───────────────── [EMAIL STUB] ─────────────────
                À      : {}
                Objet  : {}
                ---
                {}
                ─────────────────────────────────────────────────
                """, to, subject, body);
    }
}
