package com.pspd.backend.user.repository;

import com.pspd.backend.user.domain.DocumentLegal;
import com.pspd.backend.user.domain.StatutValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentLegalRepository extends JpaRepository<DocumentLegal, String> {

    List<DocumentLegal> findByPrestataireUserId(String prestataireId);

    List<DocumentLegal> findByPrestataireUserIdAndStatut(String prestataireId, StatutValidation statut);
}
