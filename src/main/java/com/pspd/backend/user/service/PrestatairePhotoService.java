package com.pspd.backend.user.service;

import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.domain.PhotoTravail;
import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.dto.PhotoResponse;
import com.pspd.backend.user.dto.PublicPrestataireResponse;
import com.pspd.backend.user.repository.PhotoTravailRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * Photos publiques du prestataire : avatar (photo de profil) + portfolio
 * (réalisations). Distinct des documents légaux (privés, validés par l'admin).
 * Stockage réutilisé via {@link FileStorageService} ; seules les images sont
 * acceptées, dans la limite de {@value #MAX_PORTFOLIO} photos de portfolio.
 */
@Service
@RequiredArgsConstructor
public class PrestatairePhotoService {

    /** Nombre maximum de photos de portfolio par prestataire. */
    public static final int MAX_PORTFOLIO = 12;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository         userRepository;
    private final PrestataireRepository  prestataireRepository;
    private final PhotoTravailRepository photoRepository;
    private final FileStorageService     fileStorageService;

    // ── Avatar ──────────────────────────────────────────────────────────────

    @Transactional
    public String setAvatar(String email, MultipartFile file) {
        requireImage(file);
        Prestataire prestataire = resolvePrestataire(email);
        String url = fileStorageService.store(file);
        prestataire.setPhotoUrl(url);
        prestataireRepository.save(prestataire);
        return "/api/prestataires/" + prestataire.getUserId() + "/avatar";
    }

    @Transactional
    public void deleteAvatar(String email) {
        Prestataire prestataire = resolvePrestataire(email);
        prestataire.setPhotoUrl(null);
        prestataireRepository.save(prestataire);
    }

    // ── Portfolio (privé : gestion par le prestataire) ───────────────────────

    @Transactional
    public PhotoResponse addPhoto(String email, MultipartFile file) {
        requireImage(file);
        Prestataire prestataire = resolvePrestataire(email);

        long count = photoRepository.countByPrestataireUserId(prestataire.getUserId());
        if (count >= MAX_PORTFOLIO) {
            throw ApiException.badRequest("PORTFOLIO_FULL",
                    "Limite de " + MAX_PORTFOLIO + " photos atteinte.");
        }

        String url = fileStorageService.store(file);
        PhotoTravail photo = PhotoTravail.builder()
                .prestataire(prestataire)
                .urlFichier(url)
                .ordre((int) count)
                .build();
        return PhotoResponse.from(photoRepository.save(photo));
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> listMine(String email) {
        Prestataire prestataire = resolvePrestataire(email);
        return listFor(prestataire.getUserId());
    }

    @Transactional
    public void deletePhoto(String email, String photoId) {
        PhotoTravail photo = photoRepository.findById(photoId)
                .orElseThrow(() -> ApiException.notFound("Photo introuvable."));
        if (!photo.getPrestataire().getUser().getEmail().equals(email)) {
            throw ApiException.forbidden("Accès refusé à cette photo.");
        }
        photoRepository.delete(photo);
    }

    // ── Lecture publique (profil prestataire visible des clients) ────────────

    @Transactional(readOnly = true)
    public List<PhotoResponse> listFor(String prestataireId) {
        return photoRepository.findByPrestataireUserIdOrderByOrdreAscCreeLeAsc(prestataireId)
                .stream().map(PhotoResponse::from).toList();
    }

    /**
     * Profil public complet (infos + avatar + portfolio) pour la page détail.
     * Le paramètre accepte indifféremment le slug (URL conviviale) ou l'UUID
     * (rétro-compatibilité des anciens liens).
     */
    @Transactional(readOnly = true)
    public PublicPrestataireResponse publicProfile(String slugOrId) {
        Prestataire prestataire = prestataireRepository.findBySlug(slugOrId)
                .or(() -> prestataireRepository.findById(slugOrId))
                .orElseThrow(() -> ApiException.notFound("Prestataire introuvable."));
        return PublicPrestataireResponse.of(prestataire, listFor(prestataire.getUserId()));
    }

    /** Chemin disque de l'avatar d'un prestataire (404 si absent). */
    @Transactional(readOnly = true)
    public String resolveAvatarPath(String prestataireId) {
        Prestataire prestataire = prestataireRepository.findById(prestataireId)
                .orElseThrow(() -> ApiException.notFound("Prestataire introuvable."));
        if (prestataire.getPhotoUrl() == null) {
            throw ApiException.notFound("Aucune photo de profil.");
        }
        return prestataire.getPhotoUrl();
    }

    /** Chemin disque d'une photo de portfolio (publique). */
    @Transactional(readOnly = true)
    public String resolvePhotoPath(String photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> ApiException.notFound("Photo introuvable."))
                .getUrlFichier();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void requireImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("FILE_EMPTY", "Le fichier est vide ou absent.");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct.toLowerCase())) {
            throw ApiException.badRequest("INVALID_IMAGE_TYPE",
                    "Format non supporté. Utilisez JPG, PNG ou WebP.");
        }
    }

    private Prestataire resolvePrestataire(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));
        return prestataireRepository.findById(user.getId())
                .orElseThrow(() -> ApiException.badRequest("NOT_PRESTATAIRE",
                        "Ce compte n'est pas un prestataire."));
    }
}
