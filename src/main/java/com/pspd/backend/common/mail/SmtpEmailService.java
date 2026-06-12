package com.pspd.backend.common.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implémentation SMTP réelle (prod), en HTML. Active quand app.mail.smtp.enabled=true.
 * Compatible avec n'importe quel SMTP gratuit (Gmail app-password, Brevo free tier, etc.).
 */
@Service
@ConditionalOnProperty(name = "app.mail.smtp.enabled", havingValue = "true")
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true → HTML
            mailSender.send(message);
            log.info("[EMAIL] Envoyé à {} — {}", to, subject);
        } catch (Exception e) {
            throw new IllegalStateException("Échec de l'envoi de l'email à " + to, e);
        }
    }
}
