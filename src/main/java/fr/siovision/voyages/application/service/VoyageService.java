package fr.siovision.voyages.application.service;


import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.VoyageParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.VoyageRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoyageService {
    @Autowired
    private VoyageRepository voyageRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private VoyageParticipantRepository voyageParticipantRepository;

    // Méthode pour créer un nouveau voyage
    public Voyage createVoyage(VoyageDTO voyageRequest) {
        // Convertir VoyageDTO en entité Voyage
        Voyage voyage = new Voyage();
        voyage.setNom(voyageRequest.getNom());
        voyage.setDescription(voyageRequest.getDescription());
        voyage.setDestination(voyageRequest.getDestination());
        voyage.setDateDepart(LocalDate.parse(voyageRequest.getDatesVoyage().getFrom()));
        voyage.setDateRetour(LocalDate.parse(voyageRequest.getDatesVoyage().getTo()));
        voyage.setNombreMaxParticipants(voyageRequest.getNombreMaxParticipants());
        voyage.setNombreMinParticipants(voyageRequest.getNombreMinParticipants());
        voyage.setDateDebutInscription(LocalDate.parse(voyageRequest.getDatesInscription().getFrom()));
        voyage.setDateFinInscription(LocalDate.parse(voyageRequest.getDatesInscription().getTo()));

        // Enregistrer le voyage dans la base de données
        return voyageRepository.save(voyage);
    }

    // Méthode pour récupérer tous les voyages
    public Page<VoyageDTO> list(Pageable pageable) {
        Page<Voyage> voyages = voyageRepository.findAll(pageable);
        return voyages.map(voyage -> {
            DateRangeDTO datesInscription = new DateRangeDTO(
                    voyage.getDateDebutInscription().toString(),
                    voyage.getDateFinInscription().toString()
            );
            DateRangeDTO datesVoyage = new DateRangeDTO(
                    voyage.getDateDepart().toString(),
                    voyage.getDateRetour().toString()
            );
            return new VoyageDTO(
                    voyage.getId(),
                    voyage.getNom(),
                    voyage.getDescription(),
                    voyage.getDestination(),
                    datesVoyage,
                    voyage.getNombreMinParticipants(),
                    voyage.getNombreMaxParticipants(),
                    datesInscription
            );
        });
    }

    public void addParticipantToVoyage(Long voyageId, VoyageParticipantRequest request) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new EntityNotFoundException("Voyage non trouvé"));

        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new EntityNotFoundException("Participant non trouvé"));

        VoyageParticipant vp = new VoyageParticipant();
        vp.setVoyage(voyage);
        vp.setParticipant(participant);
        vp.setAccompagnateur(request.getAccompagnateur() != null && request.getAccompagnateur());
        vp.setOrganisateur(request.getOrganisateur() != null && request.getOrganisateur());
        // Définir la date d'inscription à la date actuelle
        vp.setDateInscription(LocalDateTime.now());

        voyageParticipantRepository.save(vp);
    }

    public List<VoyagesOuvertsResponse> getVoyagesOuverts() {
        return voyageRepository.findVoyagesOuverts().stream()
                .map(v -> new VoyagesOuvertsResponse(
                        v.getId(),
                        v.getNom(),
                        v.getDescription(),
                        v.getDestination(),
                        v.getDateDepart().toString(),
                        v.getDateRetour().toString(),
                        v.getNombreMinParticipants(),
                        v.getNombreMaxParticipants(),
                        v.getDateDebutInscription().toString(),
                        v.getDateFinInscription().toString()
                ))
                .toList();
    }

    public void inscrireParticipant(Long voyageId, String email, VoyageInscriptionRequest request) {
        if (!Boolean.TRUE.equals(request.getJeMEngage())) {
            throw new IllegalArgumentException("Engagement requis pour s'inscrire.");
        }

        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new EntityNotFoundException("Voyage introuvable"));

        LocalDate today = LocalDate.now();
        if (today.isBefore(voyage.getDateDebutInscription()) || today.isAfter(voyage.getDateFinInscription())) {
            throw new IllegalStateException("Les inscriptions pour ce voyage ne sont pas ouvertes.");
        }

        Participant participant = participantRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Participant introuvable"));

        boolean dejaInscrit = voyageParticipantRepository.existsByVoyageAndParticipant(voyage, participant);
        if (dejaInscrit) {
            throw new IllegalStateException("Déjà inscrit à ce voyage.");
        }

        VoyageParticipant vp = new VoyageParticipant();
        vp.setVoyage(voyage);
        vp.setParticipant(participant);
        vp.setStatutInscription(StatutInscription.EN_ATTENTE);
        vp.setMessageMotivation(request.getMessageMotivation());
        vp.setDateInscription(LocalDateTime.now());
        vp.setDateEngagement(LocalDateTime.now());

        voyageParticipantRepository.save(vp);
    }

}
