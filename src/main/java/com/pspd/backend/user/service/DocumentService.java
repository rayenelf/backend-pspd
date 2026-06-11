package com.pspd.backend.user.service;

import com.pspd.backend.common.error.ApiException;
import com.pspd.backend.common.storage.FileStorageService;
import com.pspd.backend.user.domain.DocumentLegal;
import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.domain.StatutValidation;
import com.pspd.backend.user.domain.TypeDocument;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.dto.DocumentResponse;
import com.pspd.backend.user.repository.DocumentLegalRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Dépôt et consultation des documents légaux du prestataire (B9).
 * Le prestataire reste EN_ATTENTE jusqu'à validation admin.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final UserRepository          userRepository;
    private final PrestataireRepository   prestataireRepository;
    private final DocumentLegalRepository documentLegalRepository;
    private final FileStorageService      fileStorageService;

    @Transactional
    public DocumentResponse upload(String email, TypeDocument type, MultipartFile file) {
        Prestataire prestataire = resolvePrestataire(email);

        String url = fileStorageService.store(file);

        DocumentLegal doc = DocumentLegal.builder()
                .prestataire(prestataire)
                .type(type)
                .urlFichier(url)
                .statut(StatutValidation.EN_ATTENTE)
                .build();

        return DocumentResponse.from(documentLegalRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listMine(String email) {
        Prestataire prestataire = resolvePrestataire(email);
        return documentLegalRepository.findByPrestataireUserId(prestataire.getUserId())
                .stream().map(DocumentResponse::from).toList();
    }

    private Prestataire resolvePrestataire(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur introuvable."));
        return prestataireRepository.findById(user.getId())
                .orElseThrow(() -> ApiException.badRequest("NOT_PRESTATAIRE",
                        "Ce compte n'est pas un prestataire."));
    }
}
