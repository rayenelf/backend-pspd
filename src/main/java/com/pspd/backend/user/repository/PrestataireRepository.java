package com.pspd.backend.user.repository;

import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.domain.StatutValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrestataireRepository extends JpaRepository<Prestataire, String> {

    List<Prestataire> findByStatutValidation(StatutValidation statut);

    long countByStatutValidation(StatutValidation statut);

    Optional<Prestataire> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
