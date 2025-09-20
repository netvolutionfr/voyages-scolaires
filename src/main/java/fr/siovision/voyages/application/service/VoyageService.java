package fr.siovision.voyages.application.service;


import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoyageService {

    private final VoyageRepository voyageRepository;
    private final PaysRepository paysRepository;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final CurrentUserService currentUserService;
    private final VoyagePreferenceService voyagePreferenceService;

    // Méthode pour créer un nouveau voyage
    @Transactional
    public Voyage createVoyage(VoyageUpsertRequest voyageRequest) {
        Objects.requireNonNull(voyageRequest, "voyageRequest ne peut pas être null");
        validateDatesPresent(voyageRequest);

        if (voyageRequest.getId() != null) {
            throw new IllegalArgumentException("L'ID doit être null pour la création d'un nouveau voyage.");
        }

        Voyage voyage = new Voyage();
        voyage.setNom(voyageRequest.getNom());
        voyage.setDescription(voyageRequest.getDescription());
        voyage.setDestination(voyageRequest.getDestination());
        voyage.setPrixTotal(voyageRequest.getPrixTotal());
        voyage.setParticipationDesFamilles(voyageRequest.getParticipationDesFamilles());
        voyage.setNombreMaxParticipants(voyageRequest.getNombreMaxParticipants());
        voyage.setNombreMinParticipants(voyageRequest.getNombreMinParticipants());

        // Parsing sécurisée des dates
        voyage.setDateDepart(voyageRequest.getDatesVoyage().getFrom());
        voyage.setDateRetour(voyageRequest.getDatesVoyage().getTo());
        voyage.setDateDebutInscription(voyageRequest.getDatesInscription().getFrom());
        voyage.setDateFinInscription(voyageRequest.getDatesInscription().getTo());

        // Mode sondage
        voyage.setSondage(voyageRequest.isSondage());

        Pays pays = paysRepository.findById(voyageRequest.getPaysId())
                .orElseThrow(() -> new EntityNotFoundException("Pays non trouvé id=" + voyageRequest.getPaysId()));
        voyage.setPays(pays);

        // Récupérer les organisateurs
        List<UUID> organisateurIds = voyageRequest.getOrganisateurIds();
        voyage.setOrganisateurs(new ArrayList<>());
        if (organisateurIds != null) {
            for (UUID orgId : organisateurIds) {
                User organisateur = userRepository.findByPublicId(orgId)
                        .orElseThrow(() -> new EntityNotFoundException("Organisateur non trouvé idPublic=" + orgId));
                voyage.getOrganisateurs().add(organisateur);
            }
        }

        List<UUID> sectionIds = voyageRequest.getSectionIds();
        voyage.setSections(new ArrayList<>());
        if (sectionIds != null) {
            for (UUID secId : sectionIds) {
                Section section = sectionRepository.findByPublicId(secId)
                        .orElseThrow(() -> new EntityNotFoundException("Section non trouvée idPublic=" + secId));
                voyage.getSections().add(section);
            }
        }

        voyage.setCoverPhotoUrl(voyageRequest.getCoverPhotoUrl());
        voyage.setPrixTotal(voyageRequest.getPrixTotal());

        // Clonage sécurisé des formalités
        cloneFormalitesFromTemplate(voyage, pays.getFormalitesPays());

        return voyageRepository.save(voyage);
    }

    @Transactional
    public Voyage updateVoyage(Long id, VoyageUpsertRequest voyageRequest) {
        Objects.requireNonNull(id, "id ne peut pas être null");
        Objects.requireNonNull(voyageRequest, "voyageRequest ne peut pas être null");
        validateDatesPresent(voyageRequest);

        Voyage updated = voyageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voyage non trouvé id=" + id));

        updated.setNom(voyageRequest.getNom());
        updated.setDescription(voyageRequest.getDescription());
        updated.setDestination(voyageRequest.getDestination());
        updated.setPrixTotal(voyageRequest.getPrixTotal());
        updated.setParticipationDesFamilles(voyageRequest.getParticipationDesFamilles());
        updated.setNombreMaxParticipants(voyageRequest.getNombreMaxParticipants());
        updated.setNombreMinParticipants(voyageRequest.getNombreMinParticipants());

        // Parsing sécurisée des dates
        updated.setDateDepart(voyageRequest.getDatesVoyage().getFrom());
        updated.setDateRetour(voyageRequest.getDatesVoyage().getTo());
        updated.setDateDebutInscription(voyageRequest.getDatesInscription().getFrom());
        updated.setDateFinInscription(voyageRequest.getDatesInscription().getTo());

        // Mode sondage
        updated.setSondage(voyageRequest.isSondage());

        Pays pays = paysRepository.findById(voyageRequest.getPaysId())
                .orElseThrow(() -> new EntityNotFoundException("Pays non trouvé id=" + voyageRequest.getPaysId()));
        updated.setPays(pays);

        // Récupérer les organisateurs
        List<UUID> organisateurIds = voyageRequest.getOrganisateurIds();
        updated.setOrganisateurs(new ArrayList<>());
        if (organisateurIds != null) {
            for (UUID orgId : organisateurIds) {
                User organisateur = userRepository.findByPublicId(orgId)
                        .orElseThrow(() -> new EntityNotFoundException("Organisateur non trouvé idPublic=" + orgId));
                updated.getOrganisateurs().add(organisateur);
            }
        }

        List<UUID> sectionIds = voyageRequest.getSectionIds();
        updated.setSections(new ArrayList<>());
        if (sectionIds != null) {
            for (UUID secId : sectionIds) {
                Section section = sectionRepository.findByPublicId(secId)
                        .orElseThrow(() -> new EntityNotFoundException("Section non trouvée idPublic=" + secId));
                updated.getSections().add(section);
            }
        }

        updated.setCoverPhotoUrl(voyageRequest.getCoverPhotoUrl());
        updated.setPrixTotal(voyageRequest.getPrixTotal());

        // Note : les formalités existantes ne sont pas modifiées ici. La gestion des formalités
        // ajoutées manuellement ou via le template doit être faite via des endpoints dédiés.

        log.info("Voyage mis à jour id={}", updated.getId());
        return voyageRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public Page<VoyageDetailDTO> list(Pageable pageable) {
        return voyageRepository.findAll(pageable).map(this::mapToVoyageDTO);
    }

    @Transactional(readOnly = true)
    public VoyageDetailDTO getVoyageById(Long id) {
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voyage non trouvé id=" + id));

        List<FormaliteVoyageDTO> formalites = voyage.getFormalites().stream()
                .map(this::mapFormaliteToDTO)
                .toList();

        List<OrganisateurDTO> organisateurs = voyage.getOrganisateurs().stream()
                .map(org -> new OrganisateurDTO(
                        org.getPublicId(),
                        org.getNom(),
                        org.getPrenom(),
                        org.getEmail(),
                        org.getTelephone()
                ))
                .toList();
        List<SectionDTO> sections = voyage.getSections().stream()
                .map(sec -> new SectionDTO(
                        sec.getId(),
                        sec.getPublicId(),
                        sec.getLibelle(),
                        sec.getDescription()
                ))
                .toList();

        DateRangeDTO datesInscription = new DateRangeDTO(
                voyage.getDateDebutInscription(),
                voyage.getDateFinInscription()
        );

        DateRangeDTO datesVoyage = new DateRangeDTO(
                voyage.getDateDepart(),
                voyage.getDateRetour()
        );

        Long interestedCount = voyagePreferenceService.countInterestedUsers(voyage.getId());

        User currentUser = currentUserService.getCurrentUser();
        boolean interested = voyagePreferenceService.isInterested(currentUser, voyage.getId());

        return new VoyageDetailDTO(
                voyage.getId(),
                voyage.getNom(),
                voyage.getDescription(),
                voyage.getDestination(),
                voyage.getPrixTotal(),
                voyage.getParticipationDesFamilles(),
                voyage.getCoverPhotoUrl(),
                new PaysDTO(voyage.getPays().getId(), voyage.getPays().getNom()),
                datesVoyage,
                voyage.getNombreMinParticipants(),
                voyage.getNombreMaxParticipants(),
                datesInscription,
                voyage.getSondage() != null && voyage.getSondage(),
                formalites,
                organisateurs,
                interestedCount,
                sections,
                voyage.getUpdatedAt().toString(),
                interested
        );
    }

    // --- Méthodes utilitaires privées ---

    private void validateDatesPresent(VoyageUpsertRequest dto) {
        if (dto.getDatesVoyage() == null || dto.getDatesInscription() == null) {
            throw new IllegalArgumentException("Les plages de dates (voyage et inscription) sont requises.");
        }
        if (dto.getDatesVoyage().getFrom() == null || dto.getDatesVoyage().getTo() == null
                || dto.getDatesInscription().getFrom() == null || dto.getDatesInscription().getTo() == null) {
            throw new IllegalArgumentException("Tous les champs de date doivent être fournis.");
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
                fv.getStoreScan(),
                fv.getTripCondition(),
                fv.getNotes(),
                fv.isManuallyAdded()
        );
    }

    private VoyageDetailDTO mapToVoyageDTO(Voyage voyage) {
        DateRangeDTO datesInscription = new DateRangeDTO(
                voyage.getDateDebutInscription(),
                voyage.getDateFinInscription()
        );
        DateRangeDTO datesVoyage = new DateRangeDTO(
                voyage.getDateDepart(),
                voyage.getDateRetour()
        );
        List<OrganisateurDTO> organisateurs = voyage.getOrganisateurs().stream()
                .map(org -> new OrganisateurDTO(
                        org.getPublicId(),
                        org.getNom(),
                        org.getPrenom(),
                        org.getEmail(),
                        org.getTelephone()
                ))
                .toList();
        List<SectionDTO> sections = voyage.getSections().stream()
                .map(sec -> new SectionDTO(
                        sec.getId(),
                        sec.getPublicId(),
                        sec.getLibelle(),
                        sec.getDescription()
                ))
                .toList();

        Long interestedCount = voyagePreferenceService.countInterestedUsers(voyage.getId());

        User currentUser = currentUserService.getCurrentUser();
        boolean interested = voyagePreferenceService.isInterested(currentUser, voyage.getId());

        PaysDTO paysDTO = new PaysDTO(voyage.getPays().getId(), voyage.getPays().getNom());
        return new VoyageDetailDTO(
                voyage.getId(),
                voyage.getNom(),
                voyage.getDescription(),
                voyage.getDestination(),
                voyage.getPrixTotal(),
                voyage.getParticipationDesFamilles(),
                voyage.getCoverPhotoUrl(),
                paysDTO,
                datesVoyage,
                voyage.getNombreMinParticipants(),
                voyage.getNombreMaxParticipants(),
                datesInscription,
                voyage.getSondage(),
                List.of(), // Formalités non incluses dans la liste paginée
                organisateurs,
                interestedCount,
                sections,
                voyage.getUpdatedAt().toString(),
                interested
        );
    }
}
