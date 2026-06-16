package com.pspd.backend.booking.service;

import com.pspd.backend.common.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static com.pspd.backend.booking.domain.StatutReservation.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie la machine à états (US C2) conformément à {@code 12-state-reservation.png}.
 */
class ReservationStateServiceTest {

    private final ReservationStateService state = new ReservationStateService();

    @ParameterizedTest
    @CsvSource({
        "EN_ATTENTE, ACCEPTEE",
        "EN_ATTENTE, REFUSEE",
        "EN_ATTENTE, ANNULEE",
        "ACCEPTEE,   EN_COURS",
        "ACCEPTEE,   ANNULEE",
        "EN_COURS,   TERMINEE",
    })
    @DisplayName("Transitions autorisées")
    void transitionsAutorisees(String from, String to) {
        assertTrue(state.isAllowed(valueOf(from.trim()), valueOf(to.trim())));
        assertDoesNotThrow(() -> state.assertTransition(valueOf(from.trim()), valueOf(to.trim())));
    }

    @ParameterizedTest
    @CsvSource({
        "EN_ATTENTE, EN_COURS",   // on ne démarre pas sans accepter
        "EN_ATTENTE, TERMINEE",   // ni terminer
        "ACCEPTEE,   TERMINEE",   // il faut démarrer d'abord
        "ACCEPTEE,   REFUSEE",    // refuser après acceptation : interdit
        "EN_COURS,   ANNULEE",    // on n'annule plus une mission en cours
        "TERMINEE,   ACCEPTEE",   // statut terminal
        "REFUSEE,    ACCEPTEE",   // statut terminal
        "ANNULEE,    EN_COURS",   // statut terminal
    })
    @DisplayName("Transitions interdites → 422 TRANSITION_INVALIDE")
    void transitionsInterdites(String from, String to) {
        assertFalse(state.isAllowed(valueOf(from.trim()), valueOf(to.trim())));
        ApiException ex = assertThrows(ApiException.class,
            () -> state.assertTransition(valueOf(from.trim()), valueOf(to.trim())));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("TRANSITION_INVALIDE", ex.getCode());
    }

    @Test
    @DisplayName("Un statut terminal n'a aucune transition sortante")
    void statutsTerminaux() {
        for (var cible : values()) {
            assertFalse(state.isAllowed(TERMINEE, cible));
            assertFalse(state.isAllowed(REFUSEE, cible));
            assertFalse(state.isAllowed(EN_LITIGE, cible));
        }
    }
}
