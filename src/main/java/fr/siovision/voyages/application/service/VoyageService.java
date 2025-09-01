package fr.siovision.voyages.application.service;


import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.PaysRepository;
import fr.siovision.voyages.infrastructure.repository.VoyageParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.VoyageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoyageService {

    private final VoyageRepository voyageRepository;
    private final ParticipantRepository participantRepository;
    private final VoyageParticipantRepository voyageParticipantRepository;
    private final PaysRepository paysRepository;

    // Méthode pour créer un nouveau voyage
    @Transactional
    public Voyage createVoyage(VoyageDTO voyageRequest) {
        Objects.requireNonNull(voyageRequest, "voyageRequest ne peut pas être null");
        validateDatesPresent(voyageRequest);

        Voyage voyage = new Voyage();
        voyage.setNom(voyageRequest.getNom());
        voyage.setDescription(voyageRequest.getDescription());
        voyage.setDestination(voyageRequest.getDestination());
        voyage.setNombreMaxParticipants(voyageRequest.getNombreMaxParticipants());
        voyage.setNombreMinParticipants(voyageRequest.getNombreMinParticipants());

        // Parsing sécurisée des dates
        voyage.setDateDepart(parseZonedDateToLocal(voyageRequest.getDatesVoyage().getFrom(), "datesVoyage.from"));
        voyage.setDateRetour(parseZonedDateToLocal(voyageRequest.getDatesVoyage().getTo(), "datesVoyage.to"));
        voyage.setDateDebutInscription(parseZonedDateToLocal(voyageRequest.getDatesInscription().getFrom(), "datesInscription.from"));
        voyage.setDateFinInscription(parseZonedDateToLocal(voyageRequest.getDatesInscription().getTo(), "datesInscription.to"));

        Pays pays = paysRepository.findById(voyageRequest.getPaysId())
                .orElseThrow(() -> new EntityNotFoundException("Pays non trouvé id=" + voyageRequest.getPaysId()));
        voyage.setPays(pays);

        // Clonage sécurisé des formalités
        cloneFormalitesFromTemplate(voyage, pays.getFormalitesPays());

        return voyageRepository.save(voyage);
    }

    @Transactional(readOnly = true)
    public Page<VoyageDTO> list(Pageable pageable) {
        return voyageRepository.findAll(pageable).map(this::mapToVoyageDTO);
    }

    public void addParticipantToVoyage(Long voyageId, VoyageParticipantRequest request) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new EntityNotFoundException("Voyage non trouvé id=" + voyageId));

        Participant participant = participantRepository.findByPublicId(request.getParticipantId())
                .orElseThrow(() -> new EntityNotFoundException("Participant non trouvé idPublic=" + request.getParticipantId()));

        VoyageParticipant vp = new VoyageParticipant();
        vp.setVoyage(voyage);
        vp.setParticipant(participant);
        vp.setAccompagnateur(Boolean.TRUE.equals(request.getAccompagnateur()));
        vp.setOrganisateur(Boolean.TRUE.equals(request.getOrganisateur()));
        vp.setDateInscription(LocalDateTime.now());

        voyageParticipantRepository.save(vp);
    }

    @Transactional(readOnly = true)
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

    @Transactional
    public void inscrireParticipant(Long voyageId, String email, VoyageInscriptionRequest request) {
        if (!Boolean.TRUE.equals(request.getJeMEngage())) {
            throw new IllegalArgumentException("Engagement requis pour s'inscrire.");
        }

        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new EntityNotFoundException("Voyage introuvable id=" + voyageId));

        LocalDate today = LocalDate.now();
        if (today.isBefore(voyage.getDateDebutInscription()) || today.isAfter(voyage.getDateFinInscription())) {
            throw new IllegalStateException("Les inscriptions pour ce voyage ne sont pas ouvertes.");
        }

        Participant participant = participantRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Participant introuvable email=" + email));

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

    @Transactional(readOnly = true)
    public VoyageDetailDTO getVoyageById(Long id) {
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voyage non trouvé id=" + id));

        List<FormaliteVoyageDTO> formalites = voyage.getFormalites().stream()
                .map(this::mapFormaliteToDTO)
                .toList();

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
                new PaysDTO(voyage.getPays().getId(), voyage.getPays().getNom()),
                datesVoyage,
                voyage.getNombreMinParticipants(),
                voyage.getNombreMaxParticipants(),
                datesInscription,
                formalites
        );
    }

    // --- Méthodes utilitaires privées ---

    private void validateDatesPresent(VoyageDTO dto) {
        if (dto.getDatesVoyage() == null || dto.getDatesInscription() == null) {
            throw new IllegalArgumentException("Les plages de dates (voyage et inscription) sont requises.");
        }
        if (dto.getDatesVoyage().getFrom() == null || dto.getDatesVoyage().getTo() == null
                || dto.getDatesInscription().getFrom() == null || dto.getDatesInscription().getTo() == null) {
            throw new IllegalArgumentException("Tous les champs de date doivent être fournis.");
        }
    }

    private LocalDate parseZonedDateToLocal(String input, String fieldName) {
        try {
            return ZonedDateTime.parse(input).toLocalDate();
        } catch (DateTimeException e) {
            log.error("Erreur parsing date pour {}: {}", fieldName, input, e);
            throw new IllegalArgumentException("Format de date invalide pour " + fieldName + ": " + input, e);
        }
    }

    private void cloneFormalitesFromTemplate(Voyage voyage, List<FormalitePaysTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return;
        }
        for (FormalitePaysTemplate fpt : templates) {
            FormaliteVoyage fv = new FormaliteVoyage();
            fv.setAcceptedMime(new LinkedHashSet<>(fpt.getAcceptedMime() == null ? List.of() : fpt.getAcceptedMime()));
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
    }

    private FormaliteVoyageDTO mapFormaliteToDTO(FormaliteVoyage fv) {
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
    }

    private VoyageDTO mapToVoyageDTO(Voyage voyage) {
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
    }
}
