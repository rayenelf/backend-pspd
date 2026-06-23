package com.pspd.backend.booking.service;

import com.pspd.backend.booking.domain.Devis;
import com.pspd.backend.booking.domain.Reservation;
import com.pspd.backend.booking.domain.StatutDevis;
import com.pspd.backend.booking.domain.TypeReservation;
import com.pspd.backend.booking.dto.CreateDevisRequest;
import com.pspd.backend.booking.dto.DevisResponse;
import com.pspd.backend.booking.repository.DevisRepository;
import com.pspd.backend.booking.repository.ReservationRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.notification.service.NotificationService;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.pspd.backend.booking.domain.StatutReservation.ACCEPTEE;
import static com.pspd.backend.booking.domain.StatutReservation.EN_ATTENTE;

/**
 * Cycle de vie d'un devis (flow {@code AVEC_DEVIS}, Epic C — extension Sprint 3+).
 *
 * <p>Le prestataire chiffre la demande ({@link #creerDevis}) ; le client accepte
 * ({@link #accepterDevis}) ou refuse ({@link #refuserDevis}). À l'acceptation,
 * la réservation passe {@code EN_ATTENTE → ACCEPTEE} (via {@link ReservationStateService})
 * et son {@code prixConvenu} prend le montant du devis.</p>
 */
@Service
@RequiredArgsConstructor
public class DevisService {

    private final ReservationRepository    reservationRepository;
    private final DevisRepository          devisRepository;
    private final ReservationStateService  stateService;
    private final UserRepository           userRepository;
    private final NotificationService      notificationService;

    /** Prestataire émet un devis pour une demande AVEC_DEVIS encore EN_ATTENTE. */
    @Transactional
    public DevisResponse creerDevis(String reservationId, String prestataireEmail, CreateDevisRequest req) {
        Reservation r = chargerPourPrestataire(reservationId, prestataireEmail);

        if (r.getType() != TypeReservation.AVEC_DEVIS) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TYPE_INVALIDE",
                "Un devis ne peut être émis que pour une réservation de type AVEC_DEVIS.");
        }
        if (r.getStatut() != EN_ATTENTE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ETAT_INVALIDE",
                "Un devis ne peut être émis que tant que la demande est EN_ATTENTE.");
        }
        if (devisRepository.existsByReservationId(reservationId)) {
            throw ApiException.conflict("DEVIS_EXISTE", "Un devis a déjà été émis pour cette réservation.");
        }

        Devis devis = Devis.builder()
            .reservationId(reservationId)
            .montant(req.montant())
            .dureeEstimeeH(req.dureeEstimeeH())
            .conditions(req.conditions())
            .statut(StatutDevis.ENVOYE)
            .build();
        Devis saved = devisRepository.save(devis);

        notificationService.notifier(r.getClientId(),
            "Devis reçu",
            "Votre prestataire a chiffré votre demande : " + saved.getMontant() + " €. À vous d'accepter ou de refuser.");
        return DevisResponse.from(saved);
    }

    /** Lecture du devis d'une réservation (client OU prestataire concerné). */
    @Transactional(readOnly = true)
    public DevisResponse getDevis(String reservationId, String userEmail) {
        chargerPourConcerne(reservationId, userEmail);
        Devis devis = devisRepository.findByReservationId(reservationId)
            .orElseThrow(() -> ApiException.notFound("Aucun devis pour cette réservation."));
        return DevisResponse.from(devis);
    }

    /** Client accepte le devis → devis ACCEPTE + réservation ACCEPTEE (prix = montant du devis). */
    @Transactional
    public DevisResponse accepterDevis(String reservationId, String clientEmail) {
        Reservation r = chargerPourClient(reservationId, clientEmail);
        Devis devis = devisEnAttente(reservationId);

        stateService.assertTransition(r.getStatut(), ACCEPTEE);
        devis.setStatut(StatutDevis.ACCEPTE);
        r.setStatut(ACCEPTEE);
        r.setPrixConvenu(devis.getMontant());
        reservationRepository.save(r);
        Devis saved = devisRepository.save(devis);

        notificationService.notifier(r.getPrestataireId(),
            "Devis accepté",
            "Le client a accepté votre devis de " + saved.getMontant() + " €. La mission est confirmée.");
        return DevisResponse.from(saved);
    }

    /** Client refuse le devis → devis REFUSE (la réservation reste EN_ATTENTE ; le client peut l'annuler). */
    @Transactional
    public DevisResponse refuserDevis(String reservationId, String clientEmail) {
        Reservation r = chargerPourClient(reservationId, clientEmail);
        Devis devis = devisEnAttente(reservationId);

        devis.setStatut(StatutDevis.REFUSE);
        Devis saved = devisRepository.save(devis);

        notificationService.notifier(r.getPrestataireId(),
            "Devis refusé",
            "Le client a refusé votre devis de " + saved.getMontant() + " €.");
        return DevisResponse.from(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Devis devisEnAttente(String reservationId) {
        Devis devis = devisRepository.findByReservationId(reservationId)
            .orElseThrow(() -> ApiException.notFound("Aucun devis à traiter pour cette réservation."));
        if (devis.getStatut() != StatutDevis.ENVOYE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DEVIS_DEJA_TRAITE",
                "Ce devis a déjà été traité (statut « " + devis.getStatut() + " »).");
        }
        return devis;
    }

    private Reservation chargerPourPrestataire(String reservationId, String email) {
        Reservation r = trouver(reservationId);
        if (!userIdDepuisEmail(email).equals(r.getPrestataireId())) {
            throw ApiException.forbidden("Cette demande ne vous est pas attribuée.");
        }
        return r;
    }

    private Reservation chargerPourClient(String reservationId, String email) {
        Reservation r = trouver(reservationId);
        if (!userIdDepuisEmail(email).equals(r.getClientId())) {
            throw ApiException.forbidden("Cette réservation ne vous appartient pas.");
        }
        return r;
    }

    private Reservation chargerPourConcerne(String reservationId, String email) {
        Reservation r = trouver(reservationId);
        String userId = userIdDepuisEmail(email);
        if (!userId.equals(r.getClientId()) && !userId.equals(r.getPrestataireId())) {
            throw ApiException.forbidden("Vous n'êtes pas concerné par cette réservation.");
        }
        return r;
    }

    private Reservation trouver(String id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Réservation introuvable."));
    }

    private String userIdDepuisEmail(String email) {
        return userRepository.findByEmail(email)
            .map(User::getId)
            .orElseThrow(() -> ApiException.unauthorized("Utilisateur courant introuvable."));
    }
}
