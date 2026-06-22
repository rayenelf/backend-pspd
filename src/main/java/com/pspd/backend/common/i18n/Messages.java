package com.pspd.backend.common.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper de localisation (i18n).
 * Résout une clé de message dans la langue de la requête courante, déterminée
 * par l'en-tête {@code Accept-Language} (via {@link LocaleContextHolder}).
 *
 * <p>Usage : {@code messages.get("error.forbidden")}. Les libellés vivent dans
 * {@code messages.properties} (FR, défaut) et {@code messages_en.properties} (EN).</p>
 */
@Component
@RequiredArgsConstructor
public class Messages {

    private final MessageSource messageSource;

    /** Résout {@code key} (sans argument) dans la langue courante. */
    public String get(String key) {
        return get(key, (Object[]) null);
    }

    /**
     * Résout {@code key} avec des paramètres de substitution ({0}, {1}, …).
     * Si la clé est introuvable, retourne la clé elle-même plutôt que de lever.
     */
    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }
}
