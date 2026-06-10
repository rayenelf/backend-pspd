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
     * on retourne le compte existant. Sinon on crée un nouveau CLIENT.
     *
     * @param email  email vérifié fourni par Google
     * @param prenom given_name Google (peut être null)
     * @param nom    family_name Google (peut être null)
     */
    @Transactional
    public User findOrCreate(String email, String prenom, String nom) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .email(email)
                    .motDePasseHash(null)            // compte OAuth2 — pas de mot de passe
                    .telephone("pending")            // colonne NOT NULL : Google ne fournit pas le tel
                    .prenom(prenom)
                    .nom(nom)
                    .role(Role.CLIENT)
                    .statutCompte(StatutCompte.ACTIF)
                    .doubleAuthActive(false)
                    .build()
            ));
    }
}
