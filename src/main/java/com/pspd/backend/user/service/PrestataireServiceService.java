package com.pspd.backend.user.service;

import com.pspd.backend.catalog.domain.Service;
import com.pspd.backend.catalog.domain.StatutService;
import com.pspd.backend.catalog.dto.ServiceResponse;
import com.pspd.backend.catalog.repository.CategorieRepository;
import com.pspd.backend.catalog.repository.ServiceRepository;
import com.pspd.backend.user.domain.Prestataire;
import com.pspd.backend.user.domain.User;
import com.pspd.backend.user.dto.MesServicesResponse;
import com.pspd.backend.user.dto.ProposeServiceRequest;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestion des services proposés par un prestataire (Epic B) :
 * sélection parmi le catalogue approuvé + proposition d'un nouveau service
 * soumis à validation admin.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PrestataireServiceService {

    private final UserRepository        userRepository;
    private final PrestataireRepository prestataireRepository;
    private final ServiceRepository     serviceRepository;
    private final CategorieRepository   categorieRepository;

    /** Catalogue approuvé + sélection courante + propositions en attente du prestataire. */
    @Transactional(readOnly = true)
    public MesServicesResponse getMine(String email) {
        Prestataire p = loadPrestataire(email);

        List<ServiceResponse> available = serviceRepository
                .findByActifTrueAndStatutOrderByLibelleAsc(StatutService.APPROUVE)
                .stream().map(ServiceResponse::from).toList();

        List<String> selectedIds = p.getServices().stream()
                .map(Service::getId).toList();

        List<ServiceResponse> pending = p.getServices().stream()
                .filter(s -> s.getStatut() == StatutService.EN_ATTENTE)
                .map(ServiceResponse::from).toList();

        return new MesServicesResponse(available, selectedIds, pending);
    }

    /**
     * Remplace les services approuvés du prestataire par la sélection fournie.
     * Les propositions en attente du prestataire sont conservées.
     */
    @Transactional
    public void setMine(String email, List<String> serviceIds) {
        Prestataire p = loadPrestataire(email);
        List<String> ids = serviceIds == null ? List.of() : serviceIds;

        // Services approuvés sélectionnés (on ignore silencieusement les ids inconnus/non approuvés).
        Set<Service> nouveaux = serviceRepository.findAllById(ids).stream()
                .filter(s -> s.isActif() && s.getStatut() == StatutService.APPROUVE)
                .collect(Collectors.toCollection(HashSet::new));

        // On conserve les propositions en attente déjà rattachées à ce prestataire.
        p.getServices().stream()
                .filter(s -> s.getStatut() == StatutService.EN_ATTENTE)
                .forEach(nouveaux::add);

        p.setServices(nouveaux);
        prestataireRepository.save(p);
    }

    /**
     * Crée un service proposé (statut EN_ATTENTE) et le rattache au prestataire.
     * Il restera invisible au catalogue public jusqu'à validation par un admin.
     */
    @Transactional
    public ServiceResponse propose(String email, ProposeServiceRequest req) {
        Prestataire p = loadPrestataire(email);
        if (!categorieRepository.existsById(req.categorieId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie introuvable");
        }

        Service s = serviceRepository.save(Service.builder()
                .categorieId(req.categorieId())
                .libelle(req.libelle())
                .description(req.description())
                .statut(StatutService.EN_ATTENTE)
                .proposePar(p.getUserId())
                .build());

        p.getServices().add(s);
        prestataireRepository.save(p);
        return ServiceResponse.from(s);
    }

    private Prestataire loadPrestataire(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return prestataireRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil prestataire introuvable"));
    }
}
