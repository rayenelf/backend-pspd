package com.pspd.backend.admin.service;

import com.pspd.backend.admin.dto.AdminPrestataireResponse;
import com.pspd.backend.admin.dto.PrestataireStatsResponse;
import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.mail.EmailService;
import com.pspd.backend.common.mail.EmailTemplates;
import com.pspd.backend.user.domain.DocumentLegal;
import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.domain.StatutValidation;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.repository.DocumentLegalRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Back-office : validation des prestataires (B9).
 * L'admin liste les dossiers, consulte les documents, puis valide ou refuse.
 * Une décision met à jour le statut du prestataire ET de ses documents, et
 * notifie le prestataire par email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPrestataireService {

    private final PrestataireRepository   prestataireRepository;
    private final DocumentLegalRepository documentLegalRepository;
    private final EmailService            emailService;

    @Transactional(readOnly = true)
    public List<AdminPrestataireResponse> list(StatutValidation filtre) {
        List<Prestataire> prestataires = (filtre == null)
                ? prestataireRepository.findAll()
                : prestataireRepository.findByStatutValidation(filtre);
        return prestataires.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PrestataireStatsResponse stats() {
        return new PrestataireStatsResponse(
                prestataireRepository.countByStatutValidation(StatutValidation.EN_ATTENTE),
                prestataireRepository.countByStatutValidation(StatutValidation.VERIFICATION),
                prestataireRepository.countByStatutValidation(StatutValidation.VALIDE),
                prestataireRepository.countByStatutValidation(StatutValidation.SUSPENDU),
                prestataireRepository.count());
    }

    /**
     * Applique une décision de validation. Quand le dossier est tranché
     * (VALIDE ou SUSPENDU), les documents associés héritent du même statut et
     * sont horodatés ; le prestataire est notifié.
     */
    @Transactional
    public AdminPrestataireResponse decide(String prestataireId, String statutStr, String motif) {
        StatutValidation cible = parseStatut(statutStr);

        Prestataire prestataire = prestataireRepository.findById(prestataireId)
                .orElseThrow(() -> ApiException.notFound("Prestataire introuvable."));

        prestataire.setStatutValidation(cible);
        prestataireRepository.save(prestataire);

        List<DocumentLegal> docs = documentLegalRepository.findByPrestataireUserId(prestataireId);
        if (cible == StatutValidation.VALIDE || cible == StatutValidation.SUSPENDU) {
            LocalDateTime now = LocalDateTime.now();
            docs.forEach(d -> { d.setStatut(cible); d.setVerifieLe(now); });
            documentLegalRepository.saveAll(docs);
        }

        notifyDecision(prestataire.getUser(), cible, motif);
        return AdminPrestataireResponse.of(prestataire, docs);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AdminPrestataireResponse toResponse(Prestataire p) {
        List<DocumentLegal> docs = documentLegalRepository.findByPrestataireUserId(p.getUserId());
        return AdminPrestataireResponse.of(p, docs);
    }

    private void notifyDecision(User user, StatutValidation cible, String motif) {
        // Best-effort : un échec d'email ne doit pas annuler la décision admin.
        try {
            String prenom = user.getPrenom() != null ? user.getPrenom() : user.getEmail();
            if (cible == StatutValidation.VALIDE) {
                emailService.send(user.getEmail(),
                        "Votre compte prestataire est validé",
                        EmailTemplates.prestataireValide(prenom));
            } else if (cible == StatutValidation.SUSPENDU) {
                emailService.send(user.getEmail(),
                        "Votre dossier prestataire nécessite une révision",
                        EmailTemplates.prestataireRejete(prenom, motif));
            }
        } catch (Exception e) {
            log.warn("Échec de l'envoi de l'email de décision à {} : {}", user.getEmail(), e.getMessage());
        }
    }

    private StatutValidation parseStatut(String statut) {
        try {
            return StatutValidation.valueOf(statut);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("STATUT_INVALIDE",
                    "Statut invalide : attendu VALIDE, SUSPENDU, VERIFICATION ou EN_ATTENTE.");
        }
    }
}
