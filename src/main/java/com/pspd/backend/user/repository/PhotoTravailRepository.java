package com.pspd.backend.user.repository;

import com.pspd.backend.user.domain.PhotoTravail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoTravailRepository extends JpaRepository<PhotoTravail, String> {

    List<PhotoTravail> findByPrestataireUserIdOrderByOrdreAscCreeLeAsc(String prestataireId);

    long countByPrestataireUserId(String prestataireId);
}
