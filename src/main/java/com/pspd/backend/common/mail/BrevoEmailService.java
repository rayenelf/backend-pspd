package com.pspd.backend.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.mail.brevo.enabled", havingValue = "true")
@Slf4j
public class BrevoEmailService implements EmailService {

    private final RestClient restClient;
    private final String from;

    public BrevoEmailService(
            @Value("${app.mail.brevo.api-key}") String apiKey,
            @Value("${app.mail.from}") String from) {
        this.from = from;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", from),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody
        );

        restClient.post()
                .uri("/smtp/email")
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("[BREVO] Email envoyé à {} — {}", to, subject);
    }
}
