package com.pspd.backend.common.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Vérifie que les templates se construisent sans erreur de format et contiennent l'essentiel. */
class EmailTemplatesTest {

    @Test
    void verification_contient_le_lien() {
        String html = EmailTemplates.verification("Majd", "http://localhost:5173/auth/verify-email?token=ABC");
        assertThat(html).contains("Domivo").contains("token=ABC").contains("Confirmer mon email");
    }

    @Test
    void otp_contient_le_code_et_le_ttl() {
        String html = EmailTemplates.otp("Majd", "123456", 5);
        assertThat(html).contains("123456").contains("5 minutes");
    }

    @Test
    void newLogin_contient_appareil_et_ip() {
        String html = EmailTemplates.newLogin("Majd", "Chrome / Mac", "127.0.0.1");
        assertThat(html).contains("Chrome / Mac").contains("127.0.0.1");
    }

    @Test
    void twoFactorChanged_reflete_etat() {
        assertThat(EmailTemplates.twoFactorChanged("Majd", true)).contains("activée");
        assertThat(EmailTemplates.twoFactorChanged("Majd", false)).contains("désactivée");
    }

    @Test
    void echappe_le_html_des_valeurs_dynamiques() {
        String html = EmailTemplates.newLogin("<script>", "ua", "ip");
        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
