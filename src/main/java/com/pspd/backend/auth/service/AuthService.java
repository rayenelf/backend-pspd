package com.pspd.backend.auth.service;

import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.auth.dto.RegisterResponse;
import com.pspd.backend.user.domain.*;
import com.pspd.backend.user.repository.ClientRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import com.pspd.backend.common.jwt.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import com.pspd.backend.auth.dto.LoginRequest;
import com.pspd.backend.auth.dto.LoginResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PrestataireRepository prestataireRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TwoFactorService twoFactorService;
    private final EmailVerificationService emailVerificationService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionService sessionService;
    private final DeviceTrustService deviceTrustService;
    private final SecurityNotificationService securityNotificationService;

    public RegisterResponse register(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requis");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà pour cet email");
        }

        // Liste blanche : l'inscription publique ne crée QUE des comptes CLIENT ou
        // PRESTATAIRE. Toute autre valeur (ADMIN, SUPER_ADMIN, valeur inconnue) est
        // rejetée — empêche l'escalade de privilèges via le corps de la requête.
        Role role = parsePublicRole(req.getRole());

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .adresse(req.getAdresse())
                .role(role)
                .cguAcceptees(req.isCguAcceptees())
                .consentementLe(now)
                .motDePasseHash(passwordEncoder.encode(req.getMotDePasse()))
                .build();

        user = userRepository.save(user);

        if (role == Role.CLIENT) {
            TypeClient type = parseClientType(req.getType());
            if (type == TypeClient.ENTREPRISE && isBlank(req.getRaisonSociale())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La raison sociale est requise pour un client entreprise");
            }
            Client client = Client.builder()
                    .user(user)
                    .type(type)
                    .raisonSociale(req.getRaisonSociale())
                    .matriculeFiscal(req.getMatriculeFiscal())
                    .build();
            clientRepository.save(client);
        } else if (role == Role.PRESTATAIRE) {
            if (isBlank(req.getNomCommercial())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom commercial est requis");
            }
            if (isBlank(req.getCategoriePrincipale())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La catégorie d'activité est requise");
            }
            if (isBlank(req.getZoneIntervention())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La zone d'intervention est requise");
            }
            TypePrestataire typePrestataire = parsePrestataireType(req.getTypePrestataire());
            Prestataire p = Prestataire.builder()
                    .user(user)
                    .nomCommercial(req.getNomCommercial())
                    .categoriePrincipale(req.getCategoriePrincipale())
                    .zoneIntervention(req.getZoneIntervention())
                    .typePrestataire(typePrestataire)
                    .build();
            prestataireRepository.save(p);
        }

        // Envoi du lien de vérification — best-effort : un échec SMTP ne doit pas
        // faire échouer l'inscription (l'utilisateur pourra demander un renvoi).
        try {
            emailVerificationService.sendVerification(user);
        } catch (Exception e) {
            log.warn("Échec de l'envoi de l'email de vérification à {} : {}", user.getEmail(), e.getMessage());
        }

        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name());
    }

    public LoginResponse authenticate(LoginRequest req, String device, String ip, String deviceToken) {
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

        // Email non vérifié → on bloque (le front proposera de renvoyer le lien).
        if (!user.isEmailVerifie()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
        }

        // Compte suspendu ou supprimé par un administrateur → blocage.
        if (user.getStatutCompte() != StatutCompte.ACTIF) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED");
        }

        // Prestataire dont le dossier n'est pas encore VALIDÉ par l'admin → blocage
        // (cahier des charges §4 : statut En attente / Vérification / Suspendu).
        if (user.getRole() == Role.PRESTATAIRE) {
            StatutValidation statut = prestataireRepository.findById(user.getId())
                    .map(Prestataire::getStatutValidation)
                    .orElse(StatutValidation.EN_ATTENTE);
            if (statut != StatutValidation.VALIDE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PRESTATAIRE_NOT_VALIDATED");
            }
        }

        // 2FA active ET appareil non « de confiance » → challenge OTP.
        if (user.isDoubleAuthActive() && !deviceTrustService.isTrusted(user.getId(), deviceToken)) {
            twoFactorService.generateAndSendOtp(user);
            return LoginResponse.twoFactorChallenge(user.getEmail());
        }

        return issueSession(user, device, ip);
    }

    /**
     * Crée une session (sid), émet les tokens portant ce sid, et notifie en cas
     * de nouvel appareil. Réutilisé par le login direct et la vérification 2FA.
     */
    public LoginResponse issueSession(User user, String device, String ip) {
        String sid = sessionService.createSession(user.getId(), device, ip);
        securityNotificationService.notifyIfNewDevice(user, device, ip);

        Map<String, Object> claims = Map.of("sid", sid);
        return new LoginResponse(
                user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name(),
                tokenService.generateAccessToken(user, claims),
                tokenService.generateRefreshToken(user, claims),
                false);
    }

    /**
     * Rafraîchit l'access token à partir d'un refresh token valide (#5).
     * Conserve le même sid de session (vérifie qu'elle n'a pas été révoquée).
     */
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || !tokenService.isValid(refreshToken)
                || tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide ou expiré");
        }

        String sid = JwtClaims.getString(refreshToken, "sid");
        if (sid != null && !sessionService.exists(sid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session révoquée");
        }

        String email = tokenService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        if (sid != null) sessionService.touch(sid);

        Map<String, Object> claims = sid != null ? Map.of("sid", sid) : Map.of();
        return new LoginResponse(
                user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name(),
                tokenService.generateAccessToken(user, claims),
                tokenService.generateRefreshToken(user, claims),
                false);
    }

    // ── Helpers de parsing sûr des énumérations envoyées par le client ──────────

    /** N'autorise que CLIENT/PRESTATAIRE à l'inscription publique (anti-escalade). */
    private Role parsePublicRole(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle est requis");
        }
        Role role;
        try {
            role = Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide");
        }
        if (role != Role.CLIENT && role != Role.PRESTATAIRE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Inscription non autorisée pour ce rôle");
        }
        return role;
    }

    private TypeClient parseClientType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type de client est requis");
        }
        try {
            return TypeClient.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de client invalide");
        }
    }

    private TypePrestataire parsePrestataireType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type de prestataire est requis");
        }
        try {
            return TypePrestataire.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de prestataire invalide");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
