package com.pspd.backend.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Fallback (dev) : loggue l'email au lieu de l'envoyer.
 * Active uniquement si ni SMTP ni Resend ne sont activés.
 */
@Service
@ConditionalOnMissingBean(EmailService.class)
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
