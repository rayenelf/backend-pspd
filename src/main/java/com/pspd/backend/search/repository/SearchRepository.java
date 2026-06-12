package com.pspd.backend.search.repository;

import com.pspd.backend.search.dto.SearchRow;
import com.pspd.backend.user.domain.Prestataire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

/**
 * Recherche multi-critères des prestataires (B2/B3). Repository dédié au module
 * search (un même entité peut avoir plusieurs repositories Spring Data).
 *
 * Seuls les prestataires VALIDE sont retournés. Les filtres optionnels suivent
 * le motif {@code (:param IS NULL OR ...)}. Le tri est piloté par {@code :tri}
 * via un CASE (price ASC) avec la note en critère secondaire.
 */
public interface SearchRepository extends JpaRepository<Prestataire, String> {

    @Query(value = """
        SELECT new com.pspd.backend.search.dto.SearchRow(
            p.userId, p.nomCommercial, p.categoriePrincipale, p.noteMoyenne,
            p.certifie, p.langues, p.zoneIntervention, p.rayonKm,
            MIN(s.prixIndicatif))
        FROM Prestataire p JOIN p.services s
        WHERE p.statutValidation = com.pspd.backend.user.domain.StatutValidation.VALIDE
          AND s.actif = true
          AND (:serviceId IS NULL OR s.id = :serviceId)
          AND (:prixMax   IS NULL OR s.prixIndicatif <= :prixMax)
          AND (:noteMin   IS NULL OR p.noteMoyenne >= :noteMin)
          AND (:certifie  IS NULL OR p.certifie = :certifie)
          AND (:langue    IS NULL OR LOWER(p.langues) LIKE LOWER(CONCAT('%', :langue, '%')))
        GROUP BY p.userId, p.nomCommercial, p.categoriePrincipale, p.noteMoyenne,
                 p.certifie, p.langues, p.zoneIntervention, p.rayonKm
        ORDER BY
          CASE WHEN :tri = 'moinsCher' THEN MIN(s.prixIndicatif) END ASC,
          p.noteMoyenne DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT p.userId)
        FROM Prestataire p JOIN p.services s
        WHERE p.statutValidation = com.pspd.backend.user.domain.StatutValidation.VALIDE
          AND s.actif = true
          AND (:serviceId IS NULL OR s.id = :serviceId)
          AND (:prixMax   IS NULL OR s.prixIndicatif <= :prixMax)
          AND (:noteMin   IS NULL OR p.noteMoyenne >= :noteMin)
          AND (:certifie  IS NULL OR p.certifie = :certifie)
          AND (:langue    IS NULL OR LOWER(p.langues) LIKE LOWER(CONCAT('%', :langue, '%')))
        """)
    Page<SearchRow> search(
        @Param("serviceId") String serviceId,
        @Param("prixMax")   BigDecimal prixMax,
        @Param("noteMin")   BigDecimal noteMin,
        @Param("certifie")  Boolean certifie,
        @Param("langue")    String langue,
        @Param("tri")       String tri,
        Pageable pageable);
}
