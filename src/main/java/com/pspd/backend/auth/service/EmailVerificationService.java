package com.pspd.backend.auth.service;

import com.pspd.backend.auth.domain.OtpCode;
import com.pspd.backend.auth.domain.PendingRegistration;
import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.auth.repository.OtpCodeRepository;
import com.pspd.backend.auth.repository.PendingRegistrationRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.TypeClient;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final int OTP_TTL_MINUTES = 10; // 10 minutes pour la vérification d'email
    private static final int MAX_ATTEMPTS = 3;

    private final OtpCodeRepository otpCodeRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    /**
     * Envoie un code OTP pour vérifier l'email avant l'inscription
     */
    @Transactional
    public void sendEmailVerification(String email, RegisterRequest registerRequest) {
        // Vérifier que l'email n'est pas déjà utilisé
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_EXISTS", "Un compte existe déjà pour cet email.");
        }

        // Supprimer toute demande d'inscription en attente pour cet email
        pendingRegistrationRepository.findByEmail(email)
                .ifPresent(existing -> pendingRegistrationRepository.delete(existing));

        // Supprimer les anciens codes OTP pour cet email
        otpCodeRepository.findByEmailAndOtpType(email, OtpCode.OtpType.EMAIL_VERIFICATION)
                .ifPresent(existing -> otpCodeRepository.delete(existing));

        // Créer une nouvelle demande d'inscription en attente
        Role role = Role.valueOf(registerRequest.getRole());
        
        PendingRegistration pendingRegistration = new PendingRegistration();
        pendingRegistration.setEmail(email);
        pendingRegistration.setTelephone(registerRequest.getTelephone());
        pendingRegistration.setNom(registerRequest.getNom());
        pendingRegistration.setPrenom(registerRequest.getPrenom());
        pendingRegistration.setRole(role);
        pendingRegistration.setMotDePasseHash(passwordEncoder.encode(registerRequest.getMotDePasse()));

        // Ajouter les champs spécifiques selon le rôle
        if (role == Role.CLIENT) {
            pendingRegistration.setType(TypeClient.valueOf(registerRequest.getType()));
            pendingRegistration.setRaisonSociale(registerRequest.getRaisonSociale());
            pendingRegistration.setMatriculeFiscal(registerRequest.getMatriculeFiscal());
        } else if (role == Role.PRESTATAIRE) {
            pendingRegistration.setNomCommercial(registerRequest.getNomCommercial());
            pendingRegistration.setCategoriePrincipale(registerRequest.getCategoriePrincipale());
        }

        pendingRegistrationRepository.save(pendingRegistration);

        // Générer et envoyer le code OTP
        generateAndSendEmailOtp(email);
    }

    /**
     * Génère un OTP pour la vérification d'email
     */
    private void generateAndSendEmailOtp(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        OtpCode otp = OtpCode.builder()
                .userId(null) // Pas d'utilisateur encore créé
                .email(email)
                .codeHash(hashOtp(code))
                .otpType(OtpCode.OtpType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .attempts(0)
                .used(false)
                .build();
        
        otpCodeRepository.save(otp);

        // STUB Phase 1 — remplacer par SendGrid/SMTP en Phase 2
        log.info("[EMAIL VERIFICATION STUB] Code OTP pour {} : {}", email, code);
        
        // Envoyer l'email réel avec Spring Mail
        sendVerificationEmail(email, code);
    }

    public enum VerifyEmailStatus {
        SUCCESS,
        INVALID_CODE,
        EXPIRED,
        TOO_MANY_ATTEMPTS,
        NO_PENDING_REGISTRATION,
        NO_PENDING_OTP
    }

    public record VerifyEmailResult(VerifyEmailStatus status, String message) {}

    /**
     * Vérifie le code OTP pour la vérification d'email
     */
    @Transactional
    public VerifyEmailResult verifyEmailCode(String email, String code) {
        // Vérifier qu'il y a une demande d'inscription en attente
        Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmail(email);
        if (pendingOpt.isEmpty()) {
            return new VerifyEmailResult(VerifyEmailStatus.NO_PENDING_REGISTRATION, 
                    "Aucune demande d'inscription en attente pour cet email");
        }

        PendingRegistration pending = pendingOpt.get();
        
        // Vérifier que la demande n'a pas expiré
        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pending);
            return new VerifyEmailResult(VerifyEmailStatus.EXPIRED, 
                    "La demande d'inscription a expiré");
        }

        // Chercher le code OTP
        Optional<OtpCode> otpOpt = otpCodeRepository.findByEmailAndOtpType(email, OtpCode.OtpType.EMAIL_VERIFICATION);
        if (otpOpt.isEmpty()) {
            return new VerifyEmailResult(VerifyEmailStatus.NO_PENDING_OTP, 
                    "Aucun code de vérification en attente pour cet email");
        }

        OtpCode otp = otpOpt.get();

        if (otp.isUsed()) {
            return new VerifyEmailResult(VerifyEmailStatus.NO_PENDING_OTP, 
                    "Le code de vérification a déjà été utilisé");
        }
        
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new VerifyEmailResult(VerifyEmailStatus.EXPIRED, 
                    "Le code de vérification a expiré");
        }
        
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            return new VerifyEmailResult(VerifyEmailStatus.TOO_MANY_ATTEMPTS, 
                    "Trop de tentatives. Veuillez demander un nouveau code");
        }

        // Vérifier le code
        if (!hashOtp(code).equals(otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpCodeRepository.save(otp);
            return new VerifyEmailResult(VerifyEmailStatus.INVALID_CODE, 
                    "Code de vérification incorrect");
        }

        // Code valide - marquer comme utilisé
        otp.setUsed(true);
        otpCodeRepository.save(otp);

        return new VerifyEmailResult(VerifyEmailStatus.SUCCESS, 
                "Email vérifié avec succès");
    }

    /**
     * Renvoie un nouveau code OTP pour un email en attente de vérification
     */
    @Transactional
    public void resendEmailVerification(String email) {
        // Vérifier qu'il y a une demande d'inscription en attente
        if (!pendingRegistrationRepository.existsByEmail(email)) {
            throw ApiException.notFound("NO_PENDING_REGISTRATION", 
                    "Aucune demande d'inscription en attente pour cet email");
        }

        // Supprimer l'ancien code OTP
        otpCodeRepository.findByEmailAndOtpType(email, OtpCode.OtpType.EMAIL_VERIFICATION)
                .ifPresent(existing -> otpCodeRepository.delete(existing));

        // Générer un nouveau code
        generateAndSendEmailOtp(email);
    }

    /**
     * Récupère une demande d'inscription vérifiée
     */
    public Optional<PendingRegistration> getVerifiedPendingRegistration(String email) {
        return pendingRegistrationRepository.findByEmail(email)
                .filter(pending -> pending.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    /**
     * Supprime une demande d'inscription après création du compte
     */
    @Transactional
    public void deletePendingRegistration(String email) {
        // Supprimer d'abord les codes OTP associés
        otpCodeRepository.findByEmailAndOtpType(email, OtpCode.OtpType.EMAIL_VERIFICATION)
                .ifPresent(otpCodeRepository::delete);
        
        // Puis supprimer la demande d'inscription
        pendingRegistrationRepository.findByEmail(email)
                .ifPresent(pendingRegistrationRepository::delete);
        
        log.info("Demande d'inscription et codes OTP supprimés pour l'email: {}", email);
    }

    /**
     * Nettoyage des demandes expirées (à appeler périodiquement)
     */
    @Transactional
    public void cleanupExpiredRegistrations() {
        pendingRegistrationRepository.deleteExpired(LocalDateTime.now());
    }

    /**
     * Envoie un email de vérification avec le code OTP
     */
    private void sendVerificationEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Domivo - Code de vérification");
            message.setText(buildEmailContent(code));
            
            mailSender.send(message);
            log.info("Email de vérification envoyé avec succès à {}", email);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email à {} : {}", email, e.getMessage());
            // Ne pas lever d'exception pour ne pas bloquer l'inscription
            // Le code reste disponible dans les logs
        }
    }

    /**
     * Construit le contenu de l'email de vérification
     */
    private String buildEmailContent(String code) {
        return """
            Bonjour,
            
            Merci de vous être inscrit sur Domivo !
            
            Voici votre code de vérification : %s
            
            Ce code expire dans 10 minutes.
            
            Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
            
            Cordialement,
            L'équipe Domivo
            """.formatted(code);
    }

    private String hashOtp(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}