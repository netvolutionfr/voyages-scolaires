package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.exception.ProfilNotFoundException;
import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.infrastructure.dto.ParticipantProfileResponse;
import fr.siovision.voyages.infrastructure.dto.ParticipantRequest;
import fr.siovision.voyages.infrastructure.dto.ProfilRequest;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ParticipantService {
    @Autowired
    private ParticipantRepository participantRepository;

    public Participant createParticipant(ParticipantRequest participantRequest) {
        Participant participant = new Participant();

        participant.setNom(participantRequest.getNom());
        participant.setPrenom(participantRequest.getPrenom());
        participant.setSexe(participantRequest.getSexe());
        participant.setEmail(participantRequest.getEmail());
        participant.setTelephone(participantRequest.getTelephone());
        participant.setDateNaissance(participantRequest.getDateNaissance());
        participant.setSection(participantRequest.getSection());

        participant.setParent1Nom(participantRequest.getParent1Nom());
        participant.setParent1Prenom(participantRequest.getParent1Prenom());
        participant.setParent1Email(participantRequest.getParent1Email());
        participant.setParent1Telephone(participantRequest.getParent1Telephone());

        participant.setParent2Nom(participantRequest.getParent2Nom());
        participant.setParent2Prenom(participantRequest.getParent2Prenom());
        participant.setParent2Email(participantRequest.getParent2Email());
        participant.setParent2Telephone(participantRequest.getParent2Telephone());

        // Enregistrer le participant dans la base de données
        return participantRepository.save(participant);
    }

    public Iterable<Participant> getAllParticipants() {
        // Utiliser le repository pour récupérer tous les participants
        return participantRepository.findAll();
    }

    public Optional<Participant> getParticipantByEmail(String email) {
        // Vérifier si un participant avec l'email donné existe
        return participantRepository.findAll().stream()
                .filter(participant -> participant.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public ParticipantProfileResponse getMonProfil(String email) {
        // Renvoyer 404 si le participant n'existe pas
        Participant p = participantRepository.findByEmail(email)
                .orElseThrow(() -> new ProfilNotFoundException(email));

        return new ParticipantProfileResponse(
                p.getId(),
                p.getNom(),
                p.getPrenom(),
                p.getSexe(),
                p.getEmail(),
                p.getTelephone(),
                p.getDateNaissance() != null ? p.getDateNaissance().toString() : null,
                p.getSection(),
                p.getParent1Nom(),
                p.getParent1Prenom(),
                p.getParent1Email(),
                p.getParent1Telephone(),
                p.getParent2Nom(),
                p.getParent2Prenom(),
                p.getParent2Email(),
                p.getParent2Telephone()
        );
    }

    public ParticipantProfileResponse updateMonProfil(String email, String nom, String prenom, ProfilRequest profilRequest) {
        // Récupérer le participant par email
        Participant participant = participantRepository.findByEmail(email)
                .orElse(new Participant());

        // Mettre à jour les informations du participant
        // Le nom, le prénom et l'email ne sont pas modifiables
        // S'ils ne sont pas fournis, utiliser les valeurs du user connecté

        participant.setNom(nom);
        participant.setPrenom(prenom);
        participant.setEmail(email);

        // Sexe
        if (profilRequest.getSexe() != null) {
            participant.setSexe(profilRequest.getSexe());
        }
        // Téléphone
        if (profilRequest.getTelephone() != null && !profilRequest.getTelephone().isEmpty()) {
            participant.setTelephone(profilRequest.getTelephone());
        }
        // Date de naissance
        if (profilRequest.getDateNaissance() != null) {
            participant.setDateNaissance(profilRequest.getDateNaissance());
        }
        // Section
        if (profilRequest.getSection() != null && !profilRequest.getSection().isEmpty()) {
            participant.setSection(profilRequest.getSection());
        }

        // Mettre à jour les informations des parents
        // Parent 1 nom
        if (profilRequest.getParent1Nom() != null && !profilRequest.getParent1Nom().isEmpty()) {
            participant.setParent1Nom(profilRequest.getParent1Nom());
        }
        // Parent 1 prénom
        if (profilRequest.getParent1Prenom() != null && !profilRequest.getParent1Prenom().isEmpty()) {
            participant.setParent1Prenom(profilRequest.getParent1Prenom());
        }
        // Parent 1 email
        if (profilRequest.getParent1Email() != null && !profilRequest.getParent1Email().isEmpty()) {
            participant.setParent1Email(profilRequest.getParent1Email());
        }
        // Parent 1 téléphone
        if (profilRequest.getParent1Telephone() != null && !profilRequest.getParent1Telephone().isEmpty()) {
            participant.setParent1Telephone(profilRequest.getParent1Telephone());
        }

        // Parent 2 nom
        if (profilRequest.getParent2Nom() != null && !profilRequest.getParent2Nom().isEmpty()) {
            participant.setParent2Nom(profilRequest.getParent2Nom());
        }
        // Parent 2 prénom
        if (profilRequest.getParent2Prenom() != null && !profilRequest.getParent2Prenom().isEmpty()) {
            participant.setParent2Prenom(profilRequest.getParent2Prenom());
        }
        // Parent 2 email
        if (profilRequest.getParent2Email() != null && !profilRequest.getParent2Email().isEmpty()) {
            participant.setParent2Email(profilRequest.getParent2Email());
        }
        // Parent 2 téléphone
        if (profilRequest.getParent2Telephone() != null && !profilRequest.getParent2Telephone().isEmpty()) {
            participant.setParent2Telephone(profilRequest.getParent2Telephone());
        }

        // Enregistrer les modifications dans la base de données
        Participant updatedParticipant = participantRepository.save(participant);

        return new ParticipantProfileResponse(
                updatedParticipant.getId(),
                updatedParticipant.getNom(),
                updatedParticipant.getPrenom(),
                updatedParticipant.getSexe(),
                updatedParticipant.getEmail(),
                updatedParticipant.getTelephone(),
                updatedParticipant.getDateNaissance() != null ? updatedParticipant.getDateNaissance().toString() : null,
                updatedParticipant.getSection(),
                updatedParticipant.getParent1Nom(),
                updatedParticipant.getParent1Prenom(),
                updatedParticipant.getParent1Email(),
                updatedParticipant.getParent1Telephone(),
                updatedParticipant.getParent2Nom(),
                updatedParticipant.getParent2Prenom(),
                updatedParticipant.getParent2Email(),
                updatedParticipant.getParent2Telephone()
        );
    }
}
