package com.pspd.backend.auth.service;

import com.pspd.backend.auth.dto.LoginRequest;
import com.pspd.backend.auth.dto.LoginResponse;
import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.StatutCompte;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.ClientRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock ClientRepository      clientRepository;
    @Mock PrestataireRepository prestataireRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock TokenService          tokenService;
    @Mock TwoFactorService      twoFactorService;
    @Mock EmailVerificationService emailVerificationService;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock SessionService        sessionService;
    @Mock DeviceTrustService    deviceTrustService;
    @Mock SecurityNotificationService securityNotificationService;

    @InjectMocks AuthService service;

    // Petit helper : login sans device/ip ni device-token.
    private LoginResponse login(String email, String password) {
        return service.authenticate(new LoginRequest(email, password), "device", "ip", null);
    }

    // ── Inscription ───────────────────────────────────────────────────────────

    @Test
    void register_email_existant_renvoie_409() {
        RegisterRequest req = RegisterRequest.builder()
                .email("majd@example.com").role("CLIENT").type("PARTICULIER")
                .motDePasse("password123").build();
        when(userRepository.existsByEmail("majd@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("existe déjà");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_client_hache_le_mdp_et_cree_le_compte() {
        RegisterRequest req = RegisterRequest.builder()
                .email("new@example.com").role("CLIENT").type("PARTICULIER")
                .prenom("Sara").nom("Z").telephone("0600").motDePasse("password123").build();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(req);

        verify(passwordEncoder).encode("password123");
        verify(clientRepository).save(any());
    }

    @Test
    void register_role_admin_est_refuse_403() {
        RegisterRequest req = RegisterRequest.builder()
                .email("hacker@example.com").role("ADMIN").type("PARTICULIER")
                .nom("X").telephone("0600").adresse("12 rue A").motDePasse("password123").build();
        when(userRepository.existsByEmail("hacker@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non autorisée");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_role_inconnu_renvoie_400() {
        RegisterRequest req = RegisterRequest.builder()
                .email("x@example.com").role("BLA").nom("X").telephone("0600")
                .adresse("12 rue A").motDePasse("password123").build();
        when(userRepository.existsByEmail("x@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Rôle invalide");
    }

    @Test
    void register_prestataire_sans_zone_renvoie_400() {
        RegisterRequest req = RegisterRequest.builder()
                .email("pro@example.com").role("PRESTATAIRE").typePrestataire("INDIVIDUEL")
                .nom("Pro").telephone("0600").adresse("12 rue A").motDePasse("password123")
                .nomCommercial("ElecPro").categoriePrincipale("Électricité")
                /* zoneIntervention manquante */ .build();
        when(userRepository.existsByEmail("pro@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("zone d'intervention");
        verify(prestataireRepository, never()).save(any());
    }

    // ── Connexion ───────────────────────────────────────────────────────────────

    @Test
    void login_mauvais_mot_de_passe_renvoie_401() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> login("majd@example.com", "wrong"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Identifiants invalides");
    }

    @Test
    void login_email_non_verifie_renvoie_403() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).emailVerifie(false).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> login("majd@example.com", "password123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("EMAIL_NOT_VERIFIED");
    }

    @Test
    void login_compte_suspendu_renvoie_403() {
        User user = User.builder().id("u1").email("sus@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).emailVerifie(true)
                .statutCompte(StatutCompte.SUSPENDU).build();
        when(userRepository.findByEmail("sus@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> login("sus@example.com", "password123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ACCOUNT_SUSPENDED");
    }

    @Test
    void login_prestataire_non_valide_reussit_acces_limite() {
        // Un prestataire non encore validé PEUT se connecter (accès limité) afin de
        // pouvoir déposer ses documents ; le bandeau l'invite à les compléter.
        User user = User.builder().id("p1").email("pro@example.com")
                .motDePasseHash("HASH").role(Role.PRESTATAIRE).emailVerifie(true).build();
        when(userRepository.findByEmail("pro@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(sessionService.createSession(eq("p1"), any(), any())).thenReturn("sid-1");
        when(tokenService.generateAccessToken(eq(user), anyMap())).thenReturn("ACCESS");
        when(tokenService.generateRefreshToken(eq(user), anyMap())).thenReturn("REFRESH");

        LoginResponse resp = login("pro@example.com", "password123");

        assertThat(resp.getAccessToken()).isEqualTo("ACCESS");
        assertThat(resp.getRole()).isEqualTo("PRESTATAIRE");
    }

    @Test
    void login_prestataire_valide_reussit() {
        User user = User.builder().id("p2").email("provalide@example.com")
                .motDePasseHash("HASH").role(Role.PRESTATAIRE).emailVerifie(true).build();
        when(userRepository.findByEmail("provalide@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(sessionService.createSession(eq("p2"), any(), any())).thenReturn("sid-2");
        when(tokenService.generateAccessToken(eq(user), anyMap())).thenReturn("ACCESS");
        when(tokenService.generateRefreshToken(eq(user), anyMap())).thenReturn("REFRESH");

        LoginResponse resp = login("provalide@example.com", "password123");

        assertThat(resp.getAccessToken()).isEqualTo("ACCESS");
        assertThat(resp.getRole()).isEqualTo("PRESTATAIRE");
    }

    @Test
    void login_succes_renvoie_les_deux_tokens() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).emailVerifie(true).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(sessionService.createSession(eq("u1"), any(), any())).thenReturn("sid-1");
        when(tokenService.generateAccessToken(eq(user), anyMap())).thenReturn("ACCESS");
        when(tokenService.generateRefreshToken(eq(user), anyMap())).thenReturn("REFRESH");

        LoginResponse resp = login("majd@example.com", "password123");

        assertThat(resp.isTwoFactorRequired()).isFalse();
        assertThat(resp.getAccessToken()).isEqualTo("ACCESS");
        assertThat(resp.getRefreshToken()).isEqualTo("REFRESH");
        assertThat(resp.getRole()).isEqualTo("CLIENT");
    }

    @Test
    void login_avec_2fa_active_renvoie_un_challenge_sans_token() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).emailVerifie(true).doubleAuthActive(true).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        // appareil non « de confiance » → deviceTrustService.isTrusted renvoie false (défaut)

        LoginResponse resp = login("majd@example.com", "password123");

        assertThat(resp.isTwoFactorRequired()).isTrue();
        assertThat(resp.getAccessToken()).isNull();
        verify(twoFactorService).generateAndSendOtp(user);
        verify(tokenService, never()).generateAccessToken(any());
    }

    @Test
    void login_2fa_active_mais_appareil_de_confiance_saute_le_challenge() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).emailVerifie(true).doubleAuthActive(true).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(deviceTrustService.isTrusted("u1", "dev-token")).thenReturn(true);
        when(sessionService.createSession(eq("u1"), any(), any())).thenReturn("sid-1");
        when(tokenService.generateAccessToken(eq(user), anyMap())).thenReturn("ACCESS");
        when(tokenService.generateRefreshToken(eq(user), anyMap())).thenReturn("REFRESH");

        LoginResponse resp = service.authenticate(
                new LoginRequest("majd@example.com", "password123"), "device", "ip", "dev-token");

        assertThat(resp.isTwoFactorRequired()).isFalse();
        assertThat(resp.getAccessToken()).isEqualTo("ACCESS");
        verify(twoFactorService, never()).generateAndSendOtp(any());
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_token_invalide_renvoie_401() {
        when(tokenService.isValid("bad")).thenReturn(false);

        assertThatThrownBy(() -> service.refresh("bad"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void refresh_valide_emet_de_nouveaux_tokens() {
        User user = User.builder().id("u1").email("majd@example.com").role(Role.PRESTATAIRE).build();
        when(tokenService.isValid("good")).thenReturn(true);
        when(tokenService.extractEmail("good")).thenReturn("majd@example.com");
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(tokenService.generateAccessToken(eq(user), anyMap())).thenReturn("NEW_ACCESS");
        when(tokenService.generateRefreshToken(eq(user), anyMap())).thenReturn("NEW_REFRESH");

        LoginResponse resp = service.refresh("good");

        assertThat(resp.getAccessToken()).isEqualTo("NEW_ACCESS");
        assertThat(resp.getRefreshToken()).isEqualTo("NEW_REFRESH");
    }
}
