package com.pspd.backend.common.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Génère un slug URL-friendly à partir d'un libellé (ex: "ElecPro Tunis" → "elecpro-tunis").
 * Les accents sont retirés (NFD), tout passe en minuscules, les caractères non
 * alphanumériques deviennent des tirets, et les tirets en bord sont supprimés.
 */
public final class SlugUtils {

    private static final Pattern NON_LATIN       = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE      = Pattern.compile("[\\s]+");
    private static final Pattern UNDERSCORE      = Pattern.compile("_");
    private static final Pattern MULTI_DASH       = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAILING = Pattern.compile("^-|-$");

    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = WHITESPACE.matcher(normalized.toLowerCase()).replaceAll("-");
        slug = UNDERSCORE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");
        return LEADING_TRAILING.matcher(slug).replaceAll("");
    }
}
