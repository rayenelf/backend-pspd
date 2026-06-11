package com.pspd.backend.auth.service;

import com.pspd.backend.auth.dto.RegisterRequest;
import com.pspd.backend.auth.dto.RegisterResponse;
import com.pspd.backend.user.domain.*;
import com.pspd.backend.user.repository.ClientRepository;
import com.pspd.backend.user.repository.PrestataireRepository;
import com.pspd.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;

import com.pspd.backend.auth.dto.LoginRequest;
import com.pspd.backend.auth.dto.LoginResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PrestataireRepository prestataireRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponse register(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requis");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà pour cet email");
        }

        Role role = Role.valueOf(req.getRole());

        User user = User.builder()
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .role(role)
                .motDePasseHash(passwordEncoder.encode(req.getMotDePasse()))
                .build();

        user = userRepository.save(user);

        if (role == Role.CLIENT) {
            TypeClient type = TypeClient.valueOf(req.getType());
            Client client = Client.builder()
                    .user(user)
                    .type(type)
                    .raisonSociale(req.getRaisonSociale())
                    .matriculeFiscal(req.getMatriculeFiscal())
                    .build();
            clientRepository.save(client);
        } else if (role == Role.PRESTATAIRE) {
            Prestataire p = Prestataire.builder()
                    .user(user)
                    .nomCommercial(req.getNomCommercial())
                    .categoriePrincipale(req.getCategoriePrincipale())
                    .build();
            prestataireRepository.save(p);
        }

        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name());
    }

    public LoginResponse authenticate(LoginRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requis");
        }

        Optional<User> opt = userRepository.findByEmail(req.getEmail());
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }

        User user = opt.get();

        if (req.getMotDePasse() == null || !passwordEncoder.matches(req.getMotDePasse(), user.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }

        String token = UUID.randomUUID().toString();

        return new LoginResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getStatutCompte().name(), token);
    }
}
