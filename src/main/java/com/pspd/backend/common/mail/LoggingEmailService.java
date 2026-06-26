package com.pspd.backend.common.mail;

import lombok.extern.slf4j.Slf4j;

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
