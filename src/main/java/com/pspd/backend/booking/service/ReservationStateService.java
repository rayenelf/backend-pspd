package com.pspd.backend.booking.service;

import com.pspd.backend.booking.domain.StatutReservation;
import com.pspd.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.pspd.backend.booking.domain.StatutReservation.*;

/**
 * Machine à états d'une réservation (Epic C, source de vérité : {@code 12-state-reservation.png}).
 *
 * <p>Transitions autorisées en Sprint 3 (le reste est interdit → 422) :</p>
 * <pre>
 *   EN_ATTENTE → ACCEPTEE | REFUSEE | ANNULEE
 *   ACCEPTEE   → EN_COURS | ANNULEE
 *   EN_COURS   → TERMINEE
 * </pre>
 *
 * <p>Les statuts {@code REFUSEE}, {@code TERMINEE}, {@code ANNULEE}, {@code EN_LITIGE}
 * sont terminaux en Sprint 3 (aucune transition sortante). Le flux devis et la reprise
 * depuis {@code EN_LITIGE} relèvent des sprints suivants.</p>
 */
@Service
public class ReservationStateService {

    /** Table des transitions autorisées. Un statut absent de la map est terminal. */
    private static final Map<StatutReservation, Set<StatutReservation>> TRANSITIONS = Map.of(
        EN_ATTENTE, EnumSet.of(ACCEPTEE, REFUSEE, ANNULEE),
        ACCEPTEE,   EnumSet.of(EN_COURS, ANNULEE),
        EN_COURS,   EnumSet.of(TERMINEE)
    );

    /** Vrai si la transition {@code from → to} est autorisée. */
    public boolean isAllowed(StatutReservation from, StatutReservation to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Garantit que la transition est valide, sinon lève une {@link ApiException} 422.
     * @throws ApiException (422 {@code TRANSITION_INVALIDE}) si {@code from → to} est interdite.
     */
    public void assertTransition(StatutReservation from, StatutReservation to) {
        if (!isAllowed(from, to)) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "TRANSITION_INVALIDE",
                "Transition impossible : une réservation « " + from + " » ne peut pas passer à « " + to + " ».");
        }
    }
}
