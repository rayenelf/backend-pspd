package com.pspd.backend.booking.web;

import com.pspd.backend.booking.domain.Reservation;
import com.pspd.backend.booking.domain.StatutReservation;
import com.pspd.backend.booking.domain.TypeReservation;
import com.pspd.backend.booking.repository.ReservationRepository;
import com.pspd.backend.user.domain.Role;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration des endpoints de transition (US C2, Rôle B).
 * Contexte complet + MockMvc + sécurité réelle ; chaque test est transactionnel
 * (rollback) pour ne rien laisser en base. Le SMTP est mocké (pas d'I/O réseau).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationActionControllerIT {

    private static final String PRO_EMAIL    = "itest.pro@pspd.local";
    private static final String OTHER_PRO    = "itest.pro2@pspd.local";
    private static final String CLIENT_EMAIL = "itest.client@pspd.local";
    /** Service de démo présent depuis la migration V7. */
    private static final String DEMO_SERVICE = "20000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;

    private String reservationId;
    private String prestataireId;

    @BeforeEach
    void seed() {
        prestataireId = creerUser(PRO_EMAIL, Role.PRESTATAIRE).getId();
        creerUser(OTHER_PRO, Role.PRESTATAIRE);
        String clientId = creerUser(CLIENT_EMAIL, Role.CLIENT).getId();

        Reservation r = Reservation.builder()
            .clientId(clientId)
            .prestataireId(prestataireId)
            .serviceId(DEMO_SERVICE)
            .type(TypeReservation.IMMEDIATE)
            .statut(StatutReservation.EN_ATTENTE)
            .dateService(LocalDate.now().plusDays(10))
            .heureService(LocalTime.of(10, 0))
            .build();
        reservationId = reservationRepository.save(r).getId();
    }

    private User creerUser(String email, Role role) {
        return userRepository.save(User.builder()
            .email(email).telephone("0600000000").role(role)
            .nom("IT").prenom("Test").emailVerifie(true)
            .build());
    }

    // ── Cas nominaux ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = PRO_EMAIL, roles = "PRESTATAIRE")
    @DisplayName("Le prestataire accepte sa mission → 200 + statut ACCEPTEE + prix fixé")
    void accepter_ok() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}/accepter", reservationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("ACCEPTEE"))
            .andExpect(jsonPath("$.prixConvenu").isNotEmpty());
    }

    @Test
    @WithMockUser(username = PRO_EMAIL, roles = "PRESTATAIRE")
    @DisplayName("GET /mes-missions → liste les réservations du prestataire connecté")
    void mesMissions_ok() throws Exception {
        mockMvc.perform(get("/api/reservations/mes-missions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    @DisplayName("Le client annule sa réservation → 200 + statut ANNULEE")
    void annuler_ok() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}/annuler", reservationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("ANNULEE"));
    }

    // ── Machine à états : transition invalide ────────────────────────────────

    @Test
    @WithMockUser(username = PRO_EMAIL, roles = "PRESTATAIRE")
    @DisplayName("Terminer une mission EN_ATTENTE → 422 TRANSITION_INVALIDE")
    void transitionInvalide_422() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}/terminer", reservationId))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("TRANSITION_INVALIDE"));
    }

    // ── Sécurité : rôle & propriété ──────────────────────────────────────────

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    @DisplayName("Un client ne peut pas accepter une mission → 403 (rôle)")
    void clientNePeutPasAccepter_403() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}/accepter", reservationId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OTHER_PRO, roles = "PRESTATAIRE")
    @DisplayName("Un autre prestataire ne peut pas accepter une mission qui n'est pas la sienne → 403 (propriété)")
    void autrePrestataire_403() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}/accepter", reservationId))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sans authentification → 401")
    void sansAuth_401() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}/accepter", reservationId))
            .andExpect(status().isUnauthorized());
    }
}
