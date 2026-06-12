package com.pspd.backend.auth.controller;

import com.pspd.backend.auth.dto.*;
import com.pspd.backend.auth.service.AuthService;
import com.pspd.backend.auth.service.EmailVerificationService;
import com.pspd.backend.common.error.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;

    /**
     * Étape 1: Initie l'inscription et envoie le code OTP par email
     */
    @PostMapping("/send-verification")
    public ResponseEntity<EmailVerificationResponse> sendEmailVerification(@Valid @RequestBody RegisterRequest req) {
        try {
            authService.initiateRegistration(req);
            return ResponseEntity.ok(EmailVerificationResponse.success(req.getEmail()));
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du code de vérification pour {}: {}", req.getEmail(), e.getMessage());
            throw e; // Laisser le gestionnaire d'erreur global traiter l'exception
        }
    }

    /**
     * Étape 2: Vérifie le code OTP et finalise la création du compte
     */
    @PostMapping("/verify-email")
    public ResponseEntity<RegisterResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        // Vérifier le code OTP
        EmailVerificationService.VerifyEmailResult result = 
                emailVerificationService.verifyEmailCode(req.email(), req.code());

        if (result.status() != EmailVerificationService.VerifyEmailStatus.SUCCESS) {
            throw ApiException.badRequest("VERIFICATION_FAILED", result.message());
        }

        // Finaliser l'inscription
        RegisterResponse response = authService.completeRegistration(req.email());
        log.info("Compte créé avec succès pour l'email: {}", req.email());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Renvoie un nouveau code de vérification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<EmailVerificationResponse> resendEmailVerification(
            @Valid @RequestBody SendEmailVerificationRequest req) {
        try {
            emailVerificationService.resendEmailVerification(req.email());
            return ResponseEntity.ok(EmailVerificationResponse.success(req.email()));
        } catch (Exception e) {
            log.error("Erreur lors du renvoi du code de vérification pour {}: {}", req.email(), e.getMessage());
            throw e;
        }
    }

    /**
     * Supprime une demande d'inscription en attente (pour le nettoyage/debug)
     */
    @DeleteMapping("/pending-registration/{email}")
    public ResponseEntity<String> deletePendingRegistration(@PathVariable String email) {
        try {
            emailVerificationService.deletePendingRegistration(email);
            log.info("Demande d'inscription supprimée pour l'email: {}", email);
            return ResponseEntity.ok("Demande d'inscription supprimée avec succès pour: " + email);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la demande pour {}: {}", email, e.getMessage());
            throw e;
        }
    }
}