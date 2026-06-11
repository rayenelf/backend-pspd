package com.pspd.backend.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implémentation SMTP réelle (prod). Active quand {@code app.mail.smtp.enabled=true}.
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
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("[EMAIL] Envoyé à {} — {}", to, subject);
    }
}
