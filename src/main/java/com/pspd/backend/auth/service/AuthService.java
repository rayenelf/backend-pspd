package com.pspd.backend.auth.service;

import com.pspd.backend.auth.domain.PendingRegistration;
import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.auth.dto.RegisterResponse;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.user.domain.*;
import com.pspd.backend.user.repository.ClientRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import com.pspd.backend.auth.dto.LoginRequest;
import com.pspd.backend.auth.dto.LoginResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PrestataireRepository prestataireRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TwoFactorService twoFactorService;
    private final EmailVerificationService emailVerificationService;

    /**
     * DEPRECATED: Ancienne méthode d'inscription directe sans vérification d'email
     * Maintenue pour compatibilité temporaire
     */
    @Deprecated
    public RegisterResponse registerDirect(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requis");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw ApiException.conflict("EMAIL_EXISTS", "Un compte existe déjà pour cet email.");
        }

        Role role = Role.valueOf(req.getRole());

        User user = User.builder()
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .role(role)
                .motDePasseHash(passwordEncoder.encode(req.getMotDePasse()))
                .build();

        user = userRepository.save(user);

        if (role == Role.CLIENT) {
            TypeClient type = TypeClient.valueOf(req.getType());
            Client client = Client.builder()
                    .user(user)
                    .type(type)
                    .raisonSociale(req.getRaisonSociale())
                    .matriculeFiscal(req.getMatriculeFiscal())
                    .build();
            clientRepository.save(client);
        } else if (role == Role.PRESTATAIRE) {
            Prestataire p = Prestataire.builder()
                    .user(user)
                    .nomCommercial(req.getNomCommercial())
                    .categoriePrincipale(req.getCategoriePrincipale())
                    .build();
            prestataireRepository.save(p);
        }

        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name());
    }

    /**
     * Initie le processus d'inscription en envoyant un OTP de vérification d'email
     */
    public void initiateRegistration(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requis");
        }

        // Le service de vérification d'email va vérifier si l'email existe déjà
        emailVerificationService.sendEmailVerification(req.getEmail(), req);
    }

    /**
     * Finalise l'inscription après vérification de l'email
     */
    @Transactional
    public RegisterResponse completeRegistration(String email) {
        // Récupérer la demande d'inscription vérifiée
        PendingRegistration pending = emailVerificationService.getVerifiedPendingRegistration(email)
                .orElseThrow(() -> ApiException.notFound("NO_VERIFIED_REGISTRATION", 
                        "Aucune inscription vérifiée trouvée pour cet email"));

        // Vérifier une dernière fois que l'email n'est pas déjà utilisé
        if (userRepository.existsByEmail(email)) {
            emailVerificationService.deletePendingRegistration(email);
            throw ApiException.conflict("EMAIL_EXISTS", "Un compte existe déjà pour cet email.");
        }

        // Convertir les enums de PendingRegistration vers les enums de User
        Role role = Role.valueOf(pending.getRole().name());

        // Créer l'utilisateur
        User user = User.builder()
                .email(pending.getEmail())
                .telephone(pending.getTelephone())
                .nom(pending.getNom())
                .prenom(pending.getPrenom())
                .role(role)
                .motDePasseHash(pending.getMotDePasseHash()) // Déjà haché
                .build();

        user = userRepository.save(user);

        // Créer les entités spécifiques selon le rôle
        if (role == Role.CLIENT && pending.getType() != null) {
            TypeClient type = TypeClient.valueOf(pending.getType().name());
            Client client = Client.builder()
                    .user(user)
                    .type(type)
                    .raisonSociale(pending.getRaisonSociale())
                    .matriculeFiscal(pending.getMatriculeFiscal())
                    .build();
            clientRepository.save(client);
        } else if (role == Role.PRESTATAIRE) {
            Prestataire prestataire = Prestataire.builder()
                    .user(user)
                    .nomCommercial(pending.getNomCommercial())
                    .categoriePrincipale(pending.getCategoriePrincipale())
                    .build();
            prestataireRepository.save(prestataire);
        }

        // Supprimer la demande d'inscription en attente
        emailVerificationService.deletePendingRegistration(email);

        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name());
    }

    public LoginResponse authenticate(LoginRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requis");
        }

        Optional<User> opt = userRepository.findByEmail(req.getEmail());
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }

        User user = opt.get();

        if (req.getMotDePasse() == null || !passwordEncoder.matches(req.getMotDePasse(), user.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }

        // 2FA active → on envoie un OTP et on renvoie un challenge (pas de token).
        if (user.isDoubleAuthActive()) {
            twoFactorService.generateAndSendOtp(user);
            return LoginResponse.twoFactorChallenge(user.getEmail());
        }

        // JWT signé via TokenService (stub en dev, TokenServiceImpl du collègue ensuite).
        // Le front décode ce token pour lire role/uid/prenom… — un UUID ne fonctionnerait pas.
        return new LoginResponse(
                user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name(),
                tokenService.generateAccessToken(user),
                tokenService.generateRefreshToken(user),
                false);
    }

    /**
     * Rafraîchit l'access token à partir d'un refresh token valide (B6 / bug #5).
     * Rotation : émet un nouveau couple access + refresh.
     */
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || !tokenService.isValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide ou expiré");
        }

        String email = tokenService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        return new LoginResponse(
                user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name(),
                tokenService.generateAccessToken(user),
                tokenService.generateRefreshToken(user),
                false);
    }
}
