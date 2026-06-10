package com.pspd.backend.auth.service;

import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.StatutCompte;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée ou retrouve un compte à partir d'un profil OAuth2 (Google).
 * Étape B8 — tâche Majd.
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserProvisioningService {

    private final UserRepository userRepository;

    /**
     * findOrCreate : si l'email existe déjà (compte email/password OU OAuth2 antérieur),
     * on retourne le compte existant sans modifier son rôle.
     * Sinon on crée un nouveau compte avec le rôle fourni.
     *
     * @param email       email vérifié fourni par Google
     * @param prenom      given_name Google (peut être null)
     * @param nom         family_name Google (peut être null)
     * @param pendingRole rôle choisi sur la page Signup ("CLIENT" ou "PRESTATAIRE").
     *                    Ignoré si le compte existe déjà.
     *                    Null (login page) → défaut CLIENT pour un nouveau compte.
     */
    @Transactional
    public User findOrCreate(String email, String prenom, String nom, String pendingRole) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> {
                Role role = "PRESTATAIRE".equals(pendingRole) ? Role.PRESTATAIRE : Role.CLIENT;
                return userRepository.save(
                    User.builder()
                        .email(email)
                        .motDePasseHash(null)        // compte OAuth2 — pas de mot de passe
                        .telephone("pending")        // colonne NOT NULL : Google ne fournit pas le tel
                        .prenom(prenom)
                        .nom(nom)
                        .role(role)
                        .statutCompte(StatutCompte.ACTIF)
                        .doubleAuthActive(false)
                        .build()
                );
            });
    }
}
