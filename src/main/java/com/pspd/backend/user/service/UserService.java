package com.pspd.backend.user.service;

import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.dto.PrestataireProfileResponse;
import com.pspd.backend.user.dto.UpdatePrestataireRequest;
import com.pspd.backend.user.dto.UpdateUserRequest;
import com.pspd.backend.user.dto.UserResponse;
import com.pspd.backend.user.domain.StatutCompte;
import com.pspd.backend.user.repository.DocumentLegalRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import com.pspd.backend.auth.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PrestataireRepository prestataireRepository;
    private final DocumentLegalRepository documentLegalRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return UserResponse.from(findUser(email));
    }

    /** Profil professionnel du prestataire connecté (pré-remplissage + statut de validation). */
    @Transactional(readOnly = true)
    public PrestataireProfileResponse getPrestataireProfile(String email) {
        User user = findUser(email);
        Prestataire p = prestataireRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profil prestataire introuvable"));
        long nbDocs = documentLegalRepository.countByPrestataireUserId(user.getId());
        return PrestataireProfileResponse.of(p, (int) nbDocs);
    }

    @Transactional
    public UserResponse updateUser(String email, UpdateUserRequest req) {
        User user = findUser(email);
        if (req.prenom()    != null) user.setPrenom(req.prenom());
        if (req.nom()       != null) user.setNom(req.nom());
        if (req.telephone() != null) user.setTelephone(req.telephone());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void updatePrestataire(String email, UpdatePrestataireRequest req) {
        User user = findUser(email);
        Prestataire p = prestataireRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profil prestataire introuvable"));

        if (req.nomCommercial()      != null) p.setNomCommercial(req.nomCommercial());
        if (req.categoriePrincipale() != null) p.setCategoriePrincipale(req.categoriePrincipale());
        if (req.zoneIntervention()   != null) p.setZoneIntervention(req.zoneIntervention());
        if (req.rayonKm()            != null) p.setRayonKm(req.rayonKm());
        if (req.langues()            != null) p.setLangues(req.langues());
        if (req.latitude()           != null) p.setLatitude(req.latitude());
        if (req.longitude()          != null) p.setLongitude(req.longitude());
        prestataireRepository.save(p);
    }

    /**
     * Suppression de compte (RGPD — droit à l'effacement, #6).
     * Anonymise les données personnelles, passe le compte en SUPPRIME, et révoque
     * toutes les sessions. Soft-delete : on conserve la ligne pour l'intégrité
     * référentielle (réservations, factures…), sans données personnelles.
     *
     * @param password mot de passe actuel (vérifié pour les comptes locaux ; ignoré pour OAuth)
     */
    @Transactional
    public void deleteAccount(String email, String password) {
        User user = findUser(email);

        // Compte local → on exige le mot de passe. Compte OAuth (pas de hash) → pas de vérif.
        if (user.getMotDePasseHash() != null) {
            if (password == null || !passwordEncoder.matches(password, user.getMotDePasseHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mot de passe incorrect");
            }
        }

        String anon = "deleted-" + UUID.randomUUID() + "@deleted.local";
        user.setEmail(anon);
        user.setNom(null);
        user.setPrenom(null);
        user.setTelephone("deleted");
        user.setMotDePasseHash(null);
        user.setDoubleAuthActive(false);
        user.setStatutCompte(StatutCompte.SUPPRIME);
        userRepository.save(user);

        sessionService.revokeAll(user.getId(), null); // déconnecte tous les appareils
    }

    /**
     * Changement de mot de passe depuis le profil (utilisateur connecté).
     * Vérifie le mot de passe actuel (sauf compte OAuth sans mot de passe, qui en définit un).
     * Déconnecte les autres appareils ; conserve la session courante.
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword, String currentSid) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 8 caractères");
        }

        User user = findUser(email);

        // Compte avec mot de passe → on exige le mot de passe actuel.
        // Compte OAuth (pas de hash) → l'utilisateur définit son premier mot de passe.
        if (user.getMotDePasseHash() != null) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getMotDePasseHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mot de passe actuel incorrect");
            }
        }

        user.setMotDePasseHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Sécurité : déconnecter les autres appareils, garder la session courante.
        sessionService.revokeAll(user.getId(), currentSid);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}
