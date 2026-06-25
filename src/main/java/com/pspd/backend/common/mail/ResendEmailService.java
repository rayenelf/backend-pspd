package com.pspd.backend.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.mail.resend.enabled", havingValue = "true")
@Slf4j
public class ResendEmailService implements EmailService {

    private final RestClient restClient;
    private final String from;

    public ResendEmailService(
            @Value("${app.mail.resend.api-key}") String apiKey,
            @Value("${app.mail.from}") String from) {
        this.from = from;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(to),
                "subject", subject,
                "html", htmlBody
        );

        restClient.post()
                .uri("/emails")
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("[RESEND] Email envoyé à {} — {}", to, subject);
    }
}
