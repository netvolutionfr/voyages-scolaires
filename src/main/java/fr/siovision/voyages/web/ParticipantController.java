package fr.siovision.voyages.web;


import fr.siovision.voyages.application.service.KeycloakService;
import fr.siovision.voyages.application.service.ParticipantService;
import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.infrastructure.dto.ParticipantProfileResponse;
import fr.siovision.voyages.infrastructure.dto.ParticipantRequest;
import fr.siovision.voyages.infrastructure.dto.ProfilRequest;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class ParticipantController {
    @Autowired
    private ParticipantService participantService;
    @Autowired
    private KeycloakService keycloakService;

    @GetMapping("/api/participants")
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public ResponseEntity<Iterable<Participant>> getParticipants() {
        // Récupérer tous les participants
        Iterable<Participant> participants = participantService.getAllParticipants();
        return ResponseEntity.ok(participants);
    }

    @PostMapping("/api/participants")
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public ResponseEntity<Participant> createParticipant(@RequestBody ParticipantRequest participantRequest) {
        // 0. Vérifier que l'email du participant n'est pas vide
        if (participantRequest.getEmail() == null || participantRequest.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // 1. Vérifier que le participant n'existe pas déjà dans la base de données
        Optional<Participant> existingParticipant = participantService.getParticipantByEmail(participantRequest.getEmail());
        if (existingParticipant.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }

        // 2. Vérifier dans Keycloak si l'email du participant existe déjà
        UserRepresentation keycloakUser = keycloakService.findUserByEmail(participantRequest.getEmail());

        if (keycloakUser == null) {
            // 3. Créer l'utilisateur Keycloak
            keycloakUser = keycloakService.createUser(participantRequest);
        }

        // Appeler le service pour créer un nouveau participant
        Participant savedParticipant = participantService.createParticipant(participantRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedParticipant);
    }

    @GetMapping("/api/me")
    @PreAuthorize("hasAnyRole('user')")
    public ResponseEntity<ParticipantProfileResponse> getMonProfil(@AuthenticationPrincipal Jwt principal) {
        // Récupérer l'email de l'utilisateur connecté
        String email = principal.getClaimAsString("email");

        // Vérifier que l'email n'est pas vide
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // Récupérer le profil du participant
        ParticipantProfileResponse profile = participantService.getMonProfil(email);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/api/me")
    @PreAuthorize("hasAnyRole('user')")
    public ResponseEntity<ParticipantProfileResponse> updateMonProfil(@AuthenticationPrincipal Jwt principal, @RequestBody ProfilRequest profilRequest) {
        // Récupérer l'email, le nom et le prénom de l'utilisateur connecté
        String email = principal.getClaimAsString("email");
        String nom = principal.getClaimAsString("family_name");
        String prenom = principal.getClaimAsString("given_name");

        // Vérifier que l'email n'est pas vide
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // Récupérer le profil du participant
        ParticipantProfileResponse profile = participantService.updateMonProfil(email, nom, prenom, profilRequest);
        return ResponseEntity.ok(profile);
    }
}
