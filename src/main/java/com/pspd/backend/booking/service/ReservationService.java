package com.pspd.backend.booking.service;

import com.pspd.backend.booking.domain.Reservation;
import com.pspd.backend.booking.domain.StatutReservation;
import com.pspd.backend.booking.domain.TypeReservation;
import com.pspd.backend.booking.dto.CreateReservationRequest;
import com.pspd.backend.booking.dto.ReservationResponse;
import com.pspd.backend.booking.repository.ReservationRepository;
import com.pspd.backend.catalog.domain.StatutService;
import com.pspd.backend.catalog.repository.ServiceRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.user.domain.StatutValidation;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Création et lecture des réservations côté client (US C1, Dev A).
 * Distinct de {@link ReservationWorkflowService} (transitions, Dev B).
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository  reservationRepository;
    private final UserRepository         userRepository;
    private final PrestataireRepository  prestataireRepository;
    private final ServiceRepository      serviceRepository;

    @Transactional
    public ReservationResponse creer(CreateReservationRequest req, String clientEmail) {
        String clientId = userRepository.findByEmail(clientEmail)
                .map(u -> u.getId())
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));

        var service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> ApiException.notFound("Service introuvable."));
        if (!service.isActif() || service.getStatut() != StatutService.APPROUVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SERVICE_INACTIF",
                    "Ce service n'est pas disponible.");
        }

        var prestataire = prestataireRepository.findById(req.prestataireId())
                .orElseThrow(() -> ApiException.notFound("Prestataire introuvable."));
        if (prestataire.getStatutValidation() != StatutValidation.VALIDE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PRESTATAIRE_INDISPONIBLE",
                    "Ce prestataire n'accepte pas de nouvelles réservations.");
        }

        Reservation r = Reservation.builder()
                .clientId(clientId)
                .prestataireId(req.prestataireId())
                .serviceId(req.serviceId())
                .adresseId(req.adresseId())
                .dateService(req.dateService())
                .heureService(req.heureService())
                .description(req.description())
                .type(TypeReservation.IMMEDIATE)
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        return ReservationResponse.from(reservationRepository.save(r));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listerPourClient(String clientEmail, StatutReservation statut) {
        String clientId = userRepository.findByEmail(clientEmail)
                .map(u -> u.getId())
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));

        var list = (statut == null)
                ? reservationRepository.findByClientIdOrderByCreeLeDesc(clientId)
                : reservationRepository.findByClientIdAndStatutOrderByCreeLeDesc(clientId, statut);

        return list.stream().map(ReservationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(String id, String userEmail) {
        String userId = userRepository.findByEmail(userEmail)
                .map(u -> u.getId())
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));

        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Réservation introuvable."));

        if (!r.getClientId().equals(userId) && !r.getPrestataireId().equals(userId)) {
            throw ApiException.forbidden("Accès non autorisé à cette réservation.");
        }
        return ReservationResponse.from(r);
    }
}
