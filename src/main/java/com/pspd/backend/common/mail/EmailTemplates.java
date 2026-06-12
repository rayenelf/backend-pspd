package com.pspd.backend.common.mail;

/**
 * Templates HTML des emails transactionnels (brandés Domivo).
 * Styles en inline (obligatoire pour les clients mail).
 */
public final class EmailTemplates {

    private EmailTemplates() {}

    // Palette Domivo
    private static final String GRADIENT = "linear-gradient(135deg,#FF7A59,#FF5028)";
    private static final String PRIMARY  = "#FF5028";
    private static final String TEXT     = "#1f2937";
    private static final String MUTED     = "#6b7280";
    private static final String BORDER    = "#e5e7eb";

    /** Email de vérification d'adresse (lien). */
    public static String verification(String prenom, String link) {
        String body = """
            <p style="margin:0 0 16px;font-size:16px;color:%s;">Bonjour %s,</p>
            <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:%s;">
              Bienvenue sur Domivo ! Pour activer votre compte, confirmez votre adresse
              email en cliquant sur le bouton ci-dessous.
            </p>
            %s
            <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:%s;">
              Ce lien est valable <strong>24 heures</strong>. Si le bouton ne fonctionne pas,
              copiez ce lien dans votre navigateur :<br>
              <a href="%s" style="color:%s;word-break:break-all;">%s</a>
            </p>
            <p style="margin:16px 0 0;font-size:13px;color:%s;">
              Si vous n'êtes pas à l'origine de cette inscription, ignorez ce message.
            </p>
            """.formatted(TEXT, esc(prenom), MUTED, button("Confirmer mon email", link),
                          MUTED, link, PRIMARY, link, MUTED);
        return wrap("Confirmez votre adresse email", body);
    }

    /** Email contenant le code OTP de double authentification. */
    public static String otp(String prenom, String code, int ttlMinutes) {
        String body = """
            <p style="margin:0 0 16px;font-size:16px;color:%s;">Bonjour %s,</p>
            <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:%s;">
              Voici votre code de vérification pour vous connecter :
            </p>
            <div style="margin:0 auto 24px;max-width:280px;background:#fff7f4;border:1px solid %s;
                        border-radius:12px;padding:20px;text-align:center;">
              <span style="font-size:34px;font-weight:700;letter-spacing:10px;color:%s;
                           font-family:'Courier New',monospace;">%s</span>
            </div>
            <p style="margin:0 0 0;font-size:13px;line-height:1.6;color:%s;">
              Ce code expire dans <strong>%d minutes</strong>. Ne le partagez avec personne.<br>
              Si vous n'avez pas tenté de vous connecter, changez votre mot de passe immédiatement.
            </p>
            """.formatted(TEXT, esc(prenom), MUTED, BORDER, PRIMARY, esc(code), MUTED, ttlMinutes);
        return wrap("Votre code de connexion", body);
    }

    /** Email de réinitialisation de mot de passe (lien). */
    public static String passwordReset(String prenom, String link) {
        String body = """
            <p style="margin:0 0 16px;font-size:16px;color:%s;">Bonjour %s,</p>
            <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:%s;">
              Vous avez demandé à réinitialiser votre mot de passe. Cliquez sur le bouton
              ci-dessous pour en définir un nouveau.
            </p>
            %s
            <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:%s;">
              Ce lien est valable <strong>1 heure</strong>. Si le bouton ne fonctionne pas,
              copiez ce lien dans votre navigateur :<br>
              <a href="%s" style="color:%s;word-break:break-all;">%s</a>
            </p>
            <p style="margin:16px 0 0;font-size:13px;color:%s;">
              Si vous n'êtes pas à l'origine de cette demande, ignorez ce message :
              votre mot de passe reste inchangé.
            </p>
            """.formatted(TEXT, esc(prenom), MUTED, button("Réinitialiser mon mot de passe", link),
                          MUTED, link, PRIMARY, link, MUTED);
        return wrap("Réinitialisation de mot de passe", body);
    }

    /** Confirmation : le mot de passe a été changé. */
    public static String passwordChanged(String prenom) {
        String body = """
            <p style="margin:0 0 16px;font-size:16px;color:%s;">Bonjour %s,</p>
            <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:%s;">
              Votre mot de passe Domivo vient d'être modifié. Tous vos appareils ont été déconnectés
              par sécurité.
            </p>
            <p style="margin:0;font-size:13px;line-height:1.6;color:%s;">
              Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement.
            </p>
            """.formatted(TEXT, esc(prenom), MUTED, MUTED);
        return wrap("Mot de passe modifié", body);
    }

    /** Notification : nouvelle connexion depuis un appareil inconnu. */
    public static String newLogin(String prenom, String device, String ip) {
        String body = """
            <p style="margin:0 0 16px;font-size:16px;color:%s;">Bonjour %s,</p>
            <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:%s;">
              Une nouvelle connexion vient d'être effectuée sur votre compte :
            </p>
            <div style="margin:0 0 24px;background:#f9fafb;border:1px solid %s;border-radius:12px;padding:16px;">
              <p style="margin:0 0 6px;font-size:14px;color:%s;"><strong>Appareil :</strong> %s</p>
              <p style="margin:0;font-size:14px;color:%s;"><strong>Adresse IP :</strong> %s</p>
            </div>
            <p style="margin:0;font-size:13px;line-height:1.6;color:%s;">
              Si c'était vous, aucune action n'est nécessaire. Sinon, changez votre mot de passe
              et déconnectez vos appareils depuis <strong>Profil → Appareils connectés</strong>.
            </p>
            """.formatted(TEXT, esc(prenom), MUTED, BORDER, TEXT, esc(device), TEXT, esc(ip), MUTED);
        return wrap("Nouvelle connexion détectée", body);
    }

    /** Notification : activation / désactivation de la 2FA. */
    public static String twoFactorChanged(String prenom, boolean active) {
        String etat = active ? "activée" : "désactivée";
        String body = """
            <p style="margin:0 0 16px;font-size:16px;color:%s;">Bonjour %s,</p>
            <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:%s;">
              La double authentification a été <strong>%s</strong> sur votre compte Domivo.
            </p>
            <p style="margin:0;font-size:13px;line-height:1.6;color:%s;">
              Si vous n'êtes pas à l'origine de ce changement, sécurisez votre compte immédiatement
              (changez votre mot de passe et déconnectez vos appareils).
            </p>
            """.formatted(TEXT, esc(prenom), MUTED, etat, MUTED);
        return wrap("Double authentification " + etat, body);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String button(String label, String href) {
        return """
            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
              <tr><td style="border-radius:10px;background:%s;">
                <a href="%s" style="display:inline-block;padding:14px 28px;font-size:15px;
                   font-weight:600;color:#ffffff;text-decoration:none;border-radius:10px;">%s</a>
              </td></tr>
            </table>
            """.formatted(GRADIENT, href, label);
    }

    private static String wrap(String title, String body) {
        return """
            <!DOCTYPE html>
            <html lang="fr"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:24px 0;">
                <tr><td align="center">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,0.06);">
                    <tr><td style="background:%s;padding:28px 32px;">
                      <span style="font-size:22px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">Domivo</span>
                    </td></tr>
                    <tr><td style="padding:32px;">
                      <h1 style="margin:0 0 20px;font-size:20px;font-weight:700;color:%s;">%s</h1>
                      %s
                    </td></tr>
                    <tr><td style="padding:20px 32px;border-top:1px solid %s;">
                      <p style="margin:0;font-size:12px;color:%s;text-align:center;">
                        © Domivo — Services professionnels à domicile.<br>
                        Cet email vous a été envoyé automatiquement, merci de ne pas y répondre.
                      </p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
            """.formatted(GRADIENT, TEXT, title, body, BORDER, MUTED);
    }

    /** Échappement HTML minimal pour les valeurs dynamiques. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
