package com.pspd.backend.common.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService loggingEmailService() {
        return new LoggingEmailService();
    }
}
