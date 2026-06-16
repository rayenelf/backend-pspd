package com.pspd.backend.booking.service;

import com.pspd.backend.booking.domain.Reservation;
import com.pspd.backend.booking.domain.StatutReservation;
import com.pspd.backend.booking.dto.ReservationResponse;
import com.pspd.backend.booking.repository.ReservationRepository;
import com.pspd.backend.catalog.domain.Service;
import com.pspd.backend.catalog.repository.ServiceRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.notification.service.NotificationService;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.pspd.backend.booking.domain.StatutReservation.*;

/**
 * Transitions de cycle de vie d'une réservation côté prestataire/client (Dev B, US C2).
 * Chaque action contrôle la propriété (le bon acteur), vérifie la transition via
 * {@link ReservationStateService} (→ 422 si invalide), persiste, puis notifie l'autre partie.
 *
 * <p>Distinct de {@code ReservationService} (création + lecture, Dev A) pour éviter les conflits.</p>
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReservationWorkflowService {

    private final ReservationRepository  reservationRepository;
    private final ReservationStateService stateService;
    private final UserRepository         userRepository;
    private final ServiceRepository      serviceRepository;
    private final NotificationService    notificationService;

    /** Prestataire accepte la mission → ACCEPTEE. Fixe le prix convenu (prix indicatif du service). */
    @Transactional
    public ReservationResponse accepter(String reservationId, String prestataireEmail) {
        Reservation r = chargerPourPrestataire(reservationId, prestataireEmail);
        stateService.assertTransition(r.getStatut(), ACCEPTEE);
        r.setStatut(ACCEPTEE);
        r.setPrixConvenu(prixDuService(r.getServiceId()));
        Reservation saved = reservationRepository.save(r);
        notificationService.notifier(saved.getClientId(),
            "Réservation acceptée",
            "Votre prestataire a accepté votre demande du " + saved.getDateService() + ".");
        return ReservationResponse.from(saved);
    }

    /** Prestataire refuse la mission → REFUSEE. */
    @Transactional
    public ReservationResponse refuser(String reservationId, String prestataireEmail) {
        Reservation r = chargerPourPrestataire(reservationId, prestataireEmail);
        stateService.assertTransition(r.getStatut(), REFUSEE);
        r.setStatut(REFUSEE);
        Reservation saved = reservationRepository.save(r);
        notificationService.notifier(saved.getClientId(),
            "Réservation refusée",
            "Votre prestataire n'est pas disponible pour le " + saved.getDateService() + ".");
        return ReservationResponse.from(saved);
    }

    /** Prestataire démarre l'intervention → EN_COURS. (TODO Sprint 4 : exiger paiement SEQUESTRE.) */
    @Transactional
    public ReservationResponse demarrer(String reservationId, String prestataireEmail) {
        Reservation r = chargerPourPrestataire(reservationId, prestataireEmail);
        stateService.assertTransition(r.getStatut(), EN_COURS);
        r.setStatut(EN_COURS);
        Reservation saved = reservationRepository.save(r);
        notificationService.notifier(saved.getClientId(),
            "Intervention démarrée",
            "Votre prestataire a démarré l'intervention.");
        return ReservationResponse.from(saved);
    }

    /** Prestataire clôture la mission → TERMINEE. (La libération des fonds = Sprint 4.) */
    @Transactional
    public ReservationResponse terminer(String reservationId, String prestataireEmail) {
        Reservation r = chargerPourPrestataire(reservationId, prestataireEmail);
        stateService.assertTransition(r.getStatut(), TERMINEE);
        r.setStatut(TERMINEE);
        Reservation saved = reservationRepository.save(r);
        notificationService.notifier(saved.getClientId(),
            "Prestation terminée",
            "Votre prestataire a clôturé la mission. Vous pourrez bientôt la valider.");
        return ReservationResponse.from(saved);
    }

    /** Client annule sa réservation → ANNULEE (possible tant que EN_ATTENTE ou ACCEPTEE). */
    @Transactional
    public ReservationResponse annuler(String reservationId, String clientEmail) {
        Reservation r = chargerPourClient(reservationId, clientEmail);
        stateService.assertTransition(r.getStatut(), ANNULEE);
        StatutReservation precedent = r.getStatut();
        r.setStatut(ANNULEE);
        Reservation saved = reservationRepository.save(r);
        notificationService.notifier(saved.getPrestataireId(),
            "Réservation annulée",
            "Le client a annulé la réservation du " + saved.getDateService()
                + " (était « " + precedent + " »).");
        return ReservationResponse.from(saved);
    }

    /** Liste les missions du prestataire connecté (écran « Mes missions », Dev B). */
    @Transactional(readOnly = true)
    public List<ReservationResponse> mesMissions(String prestataireEmail, StatutReservation statut) {
        String prestataireId = userIdDepuisEmail(prestataireEmail);
        List<Reservation> list = (statut == null)
            ? reservationRepository.findByPrestataireIdOrderByCreeLeDesc(prestataireId)
            : reservationRepository.findByPrestataireIdAndStatutOrderByCreeLeDesc(prestataireId, statut);
        return list.stream().map(ReservationResponse::from).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Charge la réservation et vérifie que {@code email} est bien LE prestataire concerné. */
    private Reservation chargerPourPrestataire(String reservationId, String email) {
        Reservation r = trouver(reservationId);
        String userId = userIdDepuisEmail(email);
        if (!userId.equals(r.getPrestataireId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Cette mission ne vous est pas attribuée.");
        }
        return r;
    }

    /** Charge la réservation et vérifie que {@code email} est bien LE client concerné. */
    private Reservation chargerPourClient(String reservationId, String email) {
        Reservation r = trouver(reservationId);
        String userId = userIdDepuisEmail(email);
        if (!userId.equals(r.getClientId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Cette réservation ne vous appartient pas.");
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

    /** Prix convenu par défaut = prix indicatif du service (null si non renseigné). */
    private BigDecimal prixDuService(String serviceId) {
        return serviceRepository.findById(serviceId)
            .map(Service::getPrixIndicatif)
            .orElse(null);
    }
}
