package com.pspd.backend.auth.service;

import com.pspd.backend.auth.domain.OtpCode;
import com.pspd.backend.auth.repository.OtpCodeRepository;
import com.pspd.backend.auth.service.TwoFactorService.VerifyResult;
import com.pspd.backend.auth.service.TwoFactorService.VerifyStatus;
import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock OtpCodeRepository otpCodeRepository;
    @Mock UserRepository    userRepository;
    @Mock com.pspd.backend.common.mail.EmailService emailService;

    @InjectMocks TwoFactorService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-123")
                .email("majd@example.com")
                .role(Role.CLIENT)
                .doubleAuthActive(true)
                .build();
    }

    // ── Génération ──────────────────────────────────────────────────────────

    @Test
    void generateAndSendOtp_purge_les_anciens_codes() {
        service.generateAndSendOtp(user);
        verify(otpCodeRepository).deleteByUserId("user-123");
    }

    @Test
    void generateAndSendOtp_stocke_un_code_hache_a_6_chiffres_avec_ttl() {
        service.generateAndSendOtp(user);

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(captor.capture());
        OtpCode saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo("user-123");
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.isUsed()).isFalse();
        // Le code haché ne doit pas être un simple 6 chiffres en clair
        assertThat(saved.getCodeHash()).isNotBlank().doesNotMatch("\\d{6}");
        // TTL ~5 min dans le futur
        assertThat(saved.getExpiresAt())
                .isAfter(LocalDateTime.now().plusMinutes(4))
                .isBefore(LocalDateTime.now().plusMinutes(6));
    }

    // ── Vérification ────────────────────────────────────────────────────────

    @Test
    void verify_code_correct_retourne_SUCCESS_et_user() {
        OtpCode otp = validOtp(hash("123456"), 0);
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(otpCodeRepository.findTopByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(Optional.of(otp));

        VerifyResult result = service.verify("majd@example.com", "123456");

        assertThat(result.status()).isEqualTo(VerifyStatus.SUCCESS);
        assertThat(result.user()).isEqualTo(user);
        assertThat(otp.isUsed()).isTrue();   // OTP consommé
    }

    @Test
    void verify_code_incorrect_incremente_les_tentatives() {
        OtpCode otp = validOtp(hash("123456"), 0);
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(otpCodeRepository.findTopByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(Optional.of(otp));

        VerifyResult result = service.verify("majd@example.com", "000000");

        assertThat(result.status()).isEqualTo(VerifyStatus.INVALID_CODE);
        assertThat(otp.getAttempts()).isEqualTo(1);
        verify(otpCodeRepository).save(otp);
    }

    @Test
    void verify_code_expire_retourne_EXPIRED() {
        OtpCode otp = validOtp(hash("123456"), 0);
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(otpCodeRepository.findTopByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(Optional.of(otp));

        VerifyResult result = service.verify("majd@example.com", "123456");

        assertThat(result.status()).isEqualTo(VerifyStatus.EXPIRED);
    }

    @Test
    void verify_trop_de_tentatives_retourne_TOO_MANY_ATTEMPTS() {
        OtpCode otp = validOtp(hash("123456"), 3);
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(otpCodeRepository.findTopByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(Optional.of(otp));

        VerifyResult result = service.verify("majd@example.com", "123456");

        assertThat(result.status()).isEqualTo(VerifyStatus.TOO_MANY_ATTEMPTS);
    }

    @Test
    void verify_otp_deja_utilise_retourne_NO_PENDING_OTP() {
        OtpCode otp = validOtp(hash("123456"), 0);
        otp.setUsed(true);
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(otpCodeRepository.findTopByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(Optional.of(otp));

        VerifyResult result = service.verify("majd@example.com", "123456");

        assertThat(result.status()).isEqualTo(VerifyStatus.NO_PENDING_OTP);
    }

    @Test
    void verify_aucun_otp_retourne_NO_PENDING_OTP() {
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(otpCodeRepository.findTopByUserIdOrderByCreatedAtDesc("user-123")).thenReturn(Optional.empty());

        VerifyResult result = service.verify("majd@example.com", "123456");

        assertThat(result.status()).isEqualTo(VerifyStatus.NO_PENDING_OTP);
    }

    @Test
    void verify_utilisateur_inconnu_retourne_USER_NOT_FOUND() {
        when(userRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        VerifyResult result = service.verify("inconnu@example.com", "123456");

        assertThat(result.status()).isEqualTo(VerifyStatus.USER_NOT_FOUND);
        verifyNoInteractions(otpCodeRepository);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OtpCode validOtp(String codeHash, int attempts) {
        return OtpCode.builder()
                .id("otp-1")
                .userId("user-123")
                .codeHash(codeHash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(attempts)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** Réplique le hachage interne du service (SHA-256 + Base64). */
    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
