package com.pspd.backend.user.service;

import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.dto.UpdatePrestataireRequest;
import com.pspd.backend.user.dto.UpdateUserRequest;
import com.pspd.backend.user.dto.UserResponse;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PrestataireRepository prestataireRepository;

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return UserResponse.from(findUser(email));
    }

    @Transactional
    public UserResponse updateUser(String email, UpdateUserRequest req) {
        User user = findUser(email);
        if (req.prenom()    != null) user.setPrenom(req.prenom());
        if (req.nom()       != null) user.setNom(req.nom());
        if (req.telephone() != null) user.setTelephone(req.telephone());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void updatePrestataire(String email, UpdatePrestataireRequest req) {
        User user = findUser(email);
        Prestataire p = prestataireRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profil prestataire introuvable"));

        if (req.nomCommercial()      != null) p.setNomCommercial(req.nomCommercial());
        if (req.categoriePrincipale() != null) p.setCategoriePrincipale(req.categoriePrincipale());
        if (req.zoneIntervention()   != null) p.setZoneIntervention(req.zoneIntervention());
        if (req.rayonKm()            != null) p.setRayonKm(req.rayonKm());
        if (req.langues()            != null) p.setLangues(req.langues());
        prestataireRepository.save(p);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}
