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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmailVerificationServiceTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new RegisterRequest();
        testRequest.setEmail("test@example.com");
        testRequest.setTelephone("+21620123456");
        testRequest.setNom("Test");
        testRequest.setPrenom("User");
        testRequest.setRole("CLIENT");
        testRequest.setType("PARTICULIER");
        testRequest.setMotDePasse("password123");
    }

    @Test
    void sendEmailVerification_shouldCreatePendingRegistrationAndOtp() {
        // When
        emailVerificationService.sendEmailVerification(testRequest.getEmail(), testRequest);

        // Then
        Optional<PendingRegistration> pending = pendingRegistrationRepository.findByEmail(testRequest.getEmail());
        assertTrue(pending.isPresent());
        assertEquals("test@example.com", pending.get().getEmail());
        assertEquals(Role.CLIENT, pending.get().getRole());
        assertEquals(TypeClient.PARTICULIER, pending.get().getType());

        Optional<OtpCode> otp = otpCodeRepository.findByEmailAndOtpType(testRequest.getEmail(), OtpCode.OtpType.EMAIL_VERIFICATION);
        assertTrue(otp.isPresent());
        assertEquals(OtpCode.OtpType.EMAIL_VERIFICATION, otp.get().getOtpType());
        assertFalse(otp.get().isUsed());
    }

    @Test
    void sendEmailVerification_withExistingEmail_shouldThrowException() {
        // Given
        userRepository.save(com.pspd.backend.user.domain.User.builder()
                .email("test@example.com")
                .nom("Existing")
                .prenom("User")
                .role(Role.CLIENT)
                .motDePasseHash("hash")
                .build());

        // When & Then
        ApiException exception = assertThrows(ApiException.class, () ->
                emailVerificationService.sendEmailVerification(testRequest.getEmail(), testRequest));

        assertEquals("Un compte existe déjà pour cet email.", exception.getMessage());
    }

    @Test
    void verifyEmailCode_withValidCode_shouldReturnSuccess() {
        // Given
        emailVerificationService.sendEmailVerification(testRequest.getEmail(), testRequest);
        
        // Simuler un code OTP (normalement généré automatiquement)
        String testCode = "123456";
        OtpCode otp = otpCodeRepository.findByEmailAndOtpType(testRequest.getEmail(), OtpCode.OtpType.EMAIL_VERIFICATION).orElseThrow();
        otp.setCodeHash(hashOtp(testCode)); // Utiliser le même algorithme de hachage
        otpCodeRepository.save(otp);

        // When
        EmailVerificationService.VerifyEmailResult result = emailVerificationService.verifyEmailCode(testRequest.getEmail(), testCode);

        // Then
        assertEquals(EmailVerificationService.VerifyEmailStatus.SUCCESS, result.status());
        
        // Vérifier que l'OTP est marqué comme utilisé
        OtpCode updatedOtp = otpCodeRepository.findByEmailAndOtpType(testRequest.getEmail(), OtpCode.OtpType.EMAIL_VERIFICATION).orElseThrow();
        assertTrue(updatedOtp.isUsed());
    }

    @Test
    void verifyEmailCode_withInvalidCode_shouldReturnError() {
        // Given
        emailVerificationService.sendEmailVerification(testRequest.getEmail(), testRequest);

        // When
        EmailVerificationService.VerifyEmailResult result = emailVerificationService.verifyEmailCode(testRequest.getEmail(), "999999");

        // Then
        assertEquals(EmailVerificationService.VerifyEmailStatus.INVALID_CODE, result.status());
    }

    @Test
    void verifyEmailCode_withExpiredCode_shouldReturnExpired() {
        // Given
        emailVerificationService.sendEmailVerification(testRequest.getEmail(), testRequest);
        
        // Faire expirer l'OTP
        OtpCode otp = otpCodeRepository.findByEmailAndOtpType(testRequest.getEmail(), OtpCode.OtpType.EMAIL_VERIFICATION).orElseThrow();
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpCodeRepository.save(otp);

        // When
        EmailVerificationService.VerifyEmailResult result = emailVerificationService.verifyEmailCode(testRequest.getEmail(), "123456");

        // Then
        assertEquals(EmailVerificationService.VerifyEmailStatus.EXPIRED, result.status());
    }

    @Test
    void resendEmailVerification_shouldGenerateNewOtp() {
        // Given
        emailVerificationService.sendEmailVerification(testRequest.getEmail(), testRequest);
        String firstOtpHash = otpCodeRepository.findByEmailAndOtpType(testRequest.getEmail(), OtpCode.OtpType.EMAIL_VERIFICATION)
                .orElseThrow().getCodeHash();

        // When
        emailVerificationService.resendEmailVerification(testRequest.getEmail());

        // Then
        String secondOtpHash = otpCodeRepository.findByEmailAndOtpType(testRequest.getEmail(), OtpCode.OtpType.EMAIL_VERIFICATION)
                .orElseThrow().getCodeHash();
        
        assertNotEquals(firstOtpHash, secondOtpHash);
    }

    // Méthode utilitaire pour hasher un code OTP (copie de la méthode privée du service)
    private String hashOtp(String code) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}