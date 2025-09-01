package fr.siovision.voyages.application.service;


import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.PaysRepository;
import fr.siovision.voyages.infrastructure.repository.VoyageParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.VoyageRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
public class VoyageService {
    @Autowired
    private VoyageRepository voyageRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private VoyageParticipantRepository voyageParticipantRepository;
    @Autowired
    private PaysRepository paysRepository;

    // Méthode pour créer un nouveau voyage
    @Transactional
    public Voyage createVoyage(VoyageDTO voyageRequest) {
        // Convertir VoyageDTO en entité Voyage
        Voyage voyage = new Voyage();
        voyage.setNom(voyageRequest.getNom());
        voyage.setDescription(voyageRequest.getDescription());
        voyage.setDestination(voyageRequest.getDestination());
        // Parse format "2025-08-28T22:00:00.000Z"
        ZonedDateTime dateDepartZoned = ZonedDateTime.parse(voyageRequest.getDatesVoyage().getFrom());
        ZonedDateTime dateRetourZoned = ZonedDateTime.parse(voyageRequest.getDatesVoyage().getTo());
        voyage.setDateDepart(dateDepartZoned.toLocalDate());
        voyage.setDateRetour(dateRetourZoned.toLocalDate());
        voyage.setNombreMaxParticipants(voyageRequest.getNombreMaxParticipants());
        voyage.setNombreMinParticipants(voyageRequest.getNombreMinParticipants());
        // Parse format "2025-08-28T22:00:00.000Z" pour les dates d'inscription
        ZonedDateTime dateDebutInscriptionZoned = ZonedDateTime.parse(voyageRequest.getDatesInscription().getFrom());
        ZonedDateTime dateFinInscriptionZoned = ZonedDateTime.parse(voyageRequest.getDatesInscription().getTo());
        // Convertir les dates zonées en LocalDate
        voyage.setDateDebutInscription(dateDebutInscriptionZoned.toLocalDate());
        voyage.setDateFinInscription(dateFinInscriptionZoned.toLocalDate());

        Pays pays = paysRepository.findById(voyageRequest.getPaysId())
                .orElseThrow(() -> new EntityNotFoundException("Pays non trouvé"));
        voyage.setPays(pays);

        // Selon le pays, copier les formalités depuis FormalitePaysTemplate
        // Récupérer les formalités du pays
        List<FormalitePaysTemplate> formalitesPays = pays.getFormalitesPays();

        // Cloner les formalités pour le voyage
        for (FormalitePaysTemplate fpt : formalitesPays) {
            FormaliteVoyage fv = new FormaliteVoyage();
            fv.setAcceptedMime(new LinkedHashSet<>(fpt.getAcceptedMime()));      // ← copie
            fv.setDelaiConservationApresVoyage(fpt.getDelaiConservationApresVoyage());
            fv.setDelaiFournitureAvantDepart(fpt.getDelaiFournitureAvantDepart());
            fv.setMaxSizeMb(fpt.getMaxSizeMb());
            fv.setNotes(fpt.getNotes());
            fv.setRequired(fpt.isRequired());
            fv.setStoreScan(fpt.getStoreScan());

            Map<String, Object> src = fpt.getTripCondition();
            fv.setTripCondition(src == null ? new LinkedHashMap<>() : new LinkedHashMap<>(src));

            fv.setType(fpt.getType());
            fv.setTypeDocument(fpt.getTypeDocument());
            fv.setVoyage(voyage);
            fv.setManuallyAdded(false);
            fv.setSourceTemplate(fpt);

            voyage.addFormalite(fv);
        }


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
                    voyage.getPays().getId(),
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

        Participant participant = participantRepository.findByPublicId(request.getParticipantId())
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

    public VoyageDetailDTO getVoyageById(Long id) {
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voyage non trouvé"));

        List<FormaliteVoyageDTO> formalites = voyage.getFormalites().stream()
                .map(fv -> {
                    TypeDocumentDTO typeDocument = new TypeDocumentDTO(
                            fv.getTypeDocument().getId(),
                            fv.getTypeDocument().getAbr(),
                            fv.getTypeDocument().getNom(),
                            fv.getTypeDocument().getDescription()
                    );
                    return new FormaliteVoyageDTO(
                            fv.getId(),
                            typeDocument,
                            fv.getType(),
                            fv.isRequired(),
                            fv.getDelaiFournitureAvantDepart(),
                            fv.getAcceptedMime(),
                            fv.getMaxSizeMb(),
                            fv.getDelaiConservationApresVoyage(),
                            fv.isStoreScan(),
                            fv.getTripCondition(),
                            fv.getNotes(),
                            fv.isManuallyAdded()
                    );
                }).toList();

        PaysDTO pays = new PaysDTO(
                voyage.getPays().getId(),
                voyage.getPays().getNom()
        );
        DateRangeDTO datesInscription = new DateRangeDTO(
                voyage.getDateDebutInscription().toString(),
                voyage.getDateFinInscription().toString()
        );
        DateRangeDTO datesVoyage = new DateRangeDTO(
                voyage.getDateDepart().toString(),
                voyage.getDateRetour().toString()
        );

        return new VoyageDetailDTO(
                voyage.getId(),
                voyage.getNom(),
                voyage.getDescription(),
                voyage.getDestination(),
                new PaysDTO(
                        voyage.getPays().getId(),
                        voyage.getPays().getNom()
                ),
                datesVoyage,
                voyage.getNombreMinParticipants(),
                voyage.getNombreMaxParticipants(),
                datesInscription,
                formalites
        );
    }
}
