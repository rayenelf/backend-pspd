package com.pspd.backend.auth.service;

import com.pspd.backend.auth.dto.LoginRequest;
import com.pspd.backend.auth.dto.LoginResponse;
import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.user.domain.Role;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock ClientRepository      clientRepository;
    @Mock PrestataireRepository prestataireRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock TokenService          tokenService;
    @Mock TwoFactorService      twoFactorService;

    @InjectMocks AuthService service;

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

    // ── Connexion ───────────────────────────────────────────────────────────────

    @Test
    void login_mauvais_mot_de_passe_renvoie_401() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(new LoginRequest("majd@example.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Identifiants invalides");
    }

    @Test
    void login_succes_renvoie_les_deux_tokens() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(tokenService.generateAccessToken(user)).thenReturn("ACCESS");
        when(tokenService.generateRefreshToken(user)).thenReturn("REFRESH");

        LoginResponse resp = service.authenticate(new LoginRequest("majd@example.com", "password123"));

        assertThat(resp.isTwoFactorRequired()).isFalse();
        assertThat(resp.getAccessToken()).isEqualTo("ACCESS");
        assertThat(resp.getRefreshToken()).isEqualTo("REFRESH");
        assertThat(resp.getRole()).isEqualTo("CLIENT");
    }

    @Test
    void login_avec_2fa_active_renvoie_un_challenge_sans_token() {
        User user = User.builder().id("u1").email("majd@example.com")
                .motDePasseHash("HASH").role(Role.CLIENT).doubleAuthActive(true).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);

        LoginResponse resp = service.authenticate(new LoginRequest("majd@example.com", "password123"));

        assertThat(resp.isTwoFactorRequired()).isTrue();
        assertThat(resp.getAccessToken()).isNull();
        verify(twoFactorService).generateAndSendOtp(user);
        verify(tokenService, never()).generateAccessToken(any());
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
        when(tokenService.generateAccessToken(user)).thenReturn("NEW_ACCESS");
        when(tokenService.generateRefreshToken(user)).thenReturn("NEW_REFRESH");

        LoginResponse resp = service.refresh("good");

        assertThat(resp.getAccessToken()).isEqualTo("NEW_ACCESS");
        assertThat(resp.getRefreshToken()).isEqualTo("NEW_REFRESH");
    }
}
