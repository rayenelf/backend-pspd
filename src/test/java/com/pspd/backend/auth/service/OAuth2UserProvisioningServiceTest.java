package com.pspd.backend.auth.service;

import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.StatutCompte;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2UserProvisioningServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks OAuth2UserProvisioningService service;

    @Test
    void compte_existant_est_retourne_sans_modification_du_role() {
        User existing = User.builder()
                .id("u-1").email("majd@example.com").role(Role.PRESTATAIRE).build();
        when(userRepository.findByEmail("majd@example.com")).thenReturn(Optional.of(existing));

        // pendingRole=CLIENT mais le compte existe déjà en PRESTATAIRE → rôle inchangé
        User result = service.findOrCreate("majd@example.com", "Majd", "B", "CLIENT");

        assertThat(result).isSameAs(existing);
        assertThat(result.getRole()).isEqualTo(Role.PRESTATAIRE);
        verify(userRepository, never()).save(any());
    }

    @Test
    void nouveau_compte_cree_avec_le_role_demande_prestataire() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.findOrCreate("new@example.com", "Sara", "Z", "PRESTATAIRE");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getRole()).isEqualTo(Role.PRESTATAIRE);
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getStatutCompte()).isEqualTo(StatutCompte.ACTIF);
        assertThat(saved.getMotDePasseHash()).isNull();          // compte OAuth2
        assertThat(saved.getTelephone()).isEqualTo("pending");   // colonne NOT NULL
        assertThat(saved.isDoubleAuthActive()).isFalse();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void nouveau_compte_sans_role_defaut_client() {
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // pendingRole=null (login Google sans rôle) → CLIENT par défaut
        User result = service.findOrCreate("login@example.com", null, null, null);

        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
    }
}
