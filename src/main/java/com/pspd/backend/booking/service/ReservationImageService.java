package com.pspd.backend.booking.service;

import com.pspd.backend.booking.domain.Reservation;
import com.pspd.backend.booking.domain.ReservationImage;
import com.pspd.backend.booking.dto.ReservationImageResponse;
import com.pspd.backend.booking.repository.ReservationImageRepository;
import com.pspd.backend.booking.repository.ReservationRepository;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Images de travail jointes à une réservation AVEC_DEVIS.
 *
 * <p>Le <b>client</b> ajoute des photos du besoin ({@link #ajouter}) ; le
 * <b>prestataire</b> (comme le client) les consulte ({@link #lister} +
 * stream via le controller) pour chiffrer son devis. Le binaire est délégué
 * à {@link FileStorageService} ; seule l'URL relative est persistée.</p>
 */
@Service
@RequiredArgsConstructor
public class ReservationImageService {

    /** Garde-fou : pas plus de 8 images par demande. */
    private static final long MAX_IMAGES = 8;

    private final ReservationRepository      reservationRepository;
    private final ReservationImageRepository imageRepository;
    private final FileStorageService         fileStorageService;
    private final UserRepository             userRepository;

    /** Le client joint une image de travail à SA réservation. */
    @Transactional
    public ReservationImageResponse ajouter(String reservationId, String clientEmail, MultipartFile file) {
        Reservation r = chargerPourClient(reservationId, clientEmail);

        if (imageRepository.countByReservationId(reservationId) >= MAX_IMAGES) {
            throw ApiException.badRequest("TROP_D_IMAGES",
                "Vous ne pouvez pas joindre plus de " + MAX_IMAGES + " images.");
        }
        assertImage(file);

        String url = fileStorageService.store(file);
        ReservationImage img = ReservationImage.builder()
            .reservationId(reservationId)
            .url(url)
            .contentType(file.getContentType())
            .ordre((int) imageRepository.countByReservationId(reservationId))
            .build();
        return ReservationImageResponse.from(imageRepository.save(img));
    }

    /** Liste des images (métadonnées) — accessible au client ET au prestataire concernés. */
    @Transactional(readOnly = true)
    public List<ReservationImageResponse> lister(String reservationId, String userEmail) {
        chargerPourConcerne(reservationId, userEmail);
        return imageRepository.findByReservationIdOrderByOrdreAsc(reservationId).stream()
            .map(ReservationImageResponse::from)
            .toList();
    }

    /** Résout le chemin de stockage d'une image, après contrôle d'accès (client ou prestataire). */
    @Transactional(readOnly = true)
    public String resolveImagePath(String reservationId, String imageId, String userEmail) {
        chargerPourConcerne(reservationId, userEmail);
        ReservationImage img = imageRepository.findById(imageId)
            .orElseThrow(() -> ApiException.notFound("Image introuvable."));
        if (!img.getReservationId().equals(reservationId)) {
            throw ApiException.notFound("Image introuvable pour cette réservation.");
        }
        return img.getUrl();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertImage(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw ApiException.badRequest("FORMAT_INVALIDE", "Seules les images sont acceptées.");
        }
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
