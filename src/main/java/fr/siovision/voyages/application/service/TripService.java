package fr.siovision.voyages.application.service;


import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.mapper.SectionMapper;
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
public class TripService {

    private final TripRepository tripRepository;
    private final CountryRepository countryRepository;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final CurrentUserService currentUserService;
    private final TripPreferenceService tripPreferenceService;
    private final SectionMapper sectionMapper;

    // Méthode pour créer un nouveau voyage
    @Transactional
    public Trip createTrip(TripUpsertRequest tripUpsertRequest) {
        Objects.requireNonNull(tripUpsertRequest, "tripRequest cannot be null");
        validateDatesPresent(tripUpsertRequest);

        if (tripUpsertRequest.getId() != null) {
            throw new IllegalArgumentException("ID must be null when creating a new trip.");
        }

        Trip trip = new Trip();
        trip.setTitle(tripUpsertRequest.getTitle());
        trip.setDescription(tripUpsertRequest.getDescription());
        trip.setDestination(tripUpsertRequest.getDestination());
        trip.setFamilyContribution(tripUpsertRequest.getFamilyContribution());
        trip.setMaxParticipants(tripUpsertRequest.getMaxParticipants());
        trip.setMinParticipants(tripUpsertRequest.getMinParticipants());

        // Parsing sécurisée des dates
        trip.setDepartureDate(tripUpsertRequest.getTripDates().getFrom());
        trip.setReturnDate(tripUpsertRequest.getTripDates().getTo());
        trip.setRegistrationOpeningDate(tripUpsertRequest.getRegistrationDates().getFrom());
        trip.setRegistrationClosingDate(tripUpsertRequest.getRegistrationDates().getTo());

        // Mode sondage
        trip.setPoll(tripUpsertRequest.isPoll());

        Country country = countryRepository.findById(tripUpsertRequest.getCountryId())
                .orElseThrow(() -> new EntityNotFoundException("Country not found id=" + tripUpsertRequest.getCountryId()));
        trip.setCountry(country);

        // Récupérer les organisateurs
        List<UUID> chaperoneIds = tripUpsertRequest.getChaperoneIds();
        trip.setChaperones(new ArrayList<>());
        if (chaperoneIds != null) {
            for (UUID orgId : chaperoneIds) {
                User chaperone = userRepository.findByPublicId(orgId)
                        .orElseThrow(() -> new EntityNotFoundException("Chaperone not found idPublic=" + orgId));
                trip.getChaperones().add(chaperone);
            }
        }

        List<UUID> sectionIds = tripUpsertRequest.getSectionIds();
        trip.setSections(new ArrayList<>());
        if (sectionIds != null) {
            for (UUID secId : sectionIds) {
                Section section = sectionRepository.findByPublicId(secId)
                        .orElseThrow(() -> new EntityNotFoundException("Section non trouvée idPublic=" + secId));
                trip.getSections().add(section);
            }
        }

        trip.setCoverPhotoUrl(tripUpsertRequest.getCoverPhotoUrl());

        // Clonage sécurisé des formalités
        cloneFormalitesFromTemplate(trip, country.getCountryFormalities());

        return tripRepository.save(trip);
    }

    @Transactional
    public Trip updateTrip(Long id, TripUpsertRequest tripUpsertRequest) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(tripUpsertRequest, "tripRequest cannot be null");
        validateDatesPresent(tripUpsertRequest);

        Trip updated = tripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found id=" + id));

        updated.setTitle(tripUpsertRequest.getTitle());
        updated.setDescription(tripUpsertRequest.getDescription());
        updated.setDestination(tripUpsertRequest.getDestination());
        updated.setFamilyContribution(tripUpsertRequest.getFamilyContribution());
        updated.setMaxParticipants(tripUpsertRequest.getMaxParticipants());
        updated.setMinParticipants(tripUpsertRequest.getMinParticipants());

        // Parsing sécurisée des dates
        updated.setDepartureDate(tripUpsertRequest.getTripDates().getFrom());
        updated.setReturnDate(tripUpsertRequest.getTripDates().getTo());
        updated.setRegistrationOpeningDate(tripUpsertRequest.getRegistrationDates().getFrom());
        updated.setRegistrationClosingDate(tripUpsertRequest.getRegistrationDates().getTo());

        // Mode sondage
        updated.setPoll(tripUpsertRequest.isPoll());

        Country country = countryRepository.findById(tripUpsertRequest.getCountryId())
                .orElseThrow(() -> new EntityNotFoundException("Country id=" + tripUpsertRequest.getCountryId()));
        updated.setCountry(country);

        // Récupérer les organisateurs
        List<UUID> chaperoneIds = tripUpsertRequest.getChaperoneIds();
        updated.setChaperones(new ArrayList<>());
        if (chaperoneIds != null) {
            for (UUID orgId : chaperoneIds) {
                User chaperone = userRepository.findByPublicId(orgId)
                        .orElseThrow(() -> new EntityNotFoundException("Chaperone not found idPublic=" + orgId));
                updated.getChaperones().add(chaperone);
            }
        }

        List<UUID> sectionIds = tripUpsertRequest.getSectionIds();
        updated.setSections(new ArrayList<>());
        if (sectionIds != null) {
            for (UUID secId : sectionIds) {
                Section section = sectionRepository.findByPublicId(secId)
                        .orElseThrow(() -> new EntityNotFoundException("Section non trouvée idPublic=" + secId));
                updated.getSections().add(section);
            }
        }

        updated.setCoverPhotoUrl(tripUpsertRequest.getCoverPhotoUrl());

        // Note : les formalités existantes ne sont pas modifiées ici. La gestion des formalités
        // ajoutées manuellement ou via le template doit être faite via des endpoints dédiés.

        log.info("Trip updated id={}", updated.getId());
        return tripRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public Page<TripDetailDTO> list(Pageable pageable) {
        return tripRepository.findAll(pageable).map(this::mapToTripDTO);
    }

    @Transactional(readOnly = true)
    public TripDetailDTO getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voyage non trouvé id=" + id));

        List<TripFormalityDTO> formalites = trip.getFormalities().stream()
                .map(this::mapFormalityToDTO)
                .toList();

        List<ChaperoneDTO> organisateurs = trip.getChaperones().stream()
                .map(org -> new ChaperoneDTO(
                        org.getPublicId(),
                        org.getFirstName(),
                        org.getLastName(),
                        org.getEmail(),
                        org.getTelephone()
                ))
                .toList();
        List<SectionDTO> sections = trip.getSections().stream()
                .map(sectionMapper::toDTO)
                .toList();

        DateRangeDTO datesInscription = new DateRangeDTO(
                trip.getRegistrationOpeningDate(),
                trip.getRegistrationClosingDate()
        );

        DateRangeDTO datesVoyage = new DateRangeDTO(
                trip.getDepartureDate(),
                trip.getReturnDate()
        );

        Long interestedCount = tripPreferenceService.countInterestedUsers(trip.getId());

        User currentUser = currentUserService.getCurrentUser();
        boolean interested = tripPreferenceService.isInterested(currentUser, trip.getId());

        return new TripDetailDTO(
                trip.getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getDestination(),
                trip.getFamilyContribution(),
                trip.getCoverPhotoUrl(),
                new CountryDTO(trip.getCountry().getId(), trip.getCountry().getName()),
                datesVoyage,
                trip.getMinParticipants(),
                trip.getMaxParticipants(),
                datesInscription,
                trip.getPoll() != null && trip.getPoll(),
                formalites,
                organisateurs,
                interestedCount,
                sections,
                interested,
                trip.getUpdatedAt().toString()
        );
    }

    // --- Méthodes utilitaires privées ---

    private void validateDatesPresent(TripUpsertRequest dto) {
        if (dto.getTripDates() == null || dto.getRegistrationDates() == null) {
            throw new IllegalArgumentException("Date ranges must be provided.");
        }
        if (dto.getTripDates().getFrom() == null || dto.getTripDates().getTo() == null
                || dto.getRegistrationDates().getFrom() == null || dto.getRegistrationDates().getTo() == null) {
            throw new IllegalArgumentException("All date fields must be provided and valid.");
        }
    }

    private void cloneFormalitesFromTemplate(Trip trip, List<CountryFormalityTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return;
        }
        for (CountryFormalityTemplate fpt : templates) {
            TripFormality fv = new TripFormality();
            fv.setAcceptedMime(new LinkedHashSet<>(fpt.getAcceptedMime() == null ? List.of() : fpt.getAcceptedMime()));
            fv.setDaysRetentionAfterTrip(fpt.getDaysRetentionAfterTrip());
            fv.setDaysBeforeTrip(fpt.getDaysBeforeTrip());
            fv.setMaxSizeMb(fpt.getMaxSizeMb());
            fv.setNotes(fpt.getNotes());
            fv.setRequired(fpt.isRequired());
            fv.setStoreScan(fpt.getStoreScan());
            Map<String, Object> src = fpt.getTripCondition();
            fv.setTripCondition(src == null ? new LinkedHashMap<>() : new LinkedHashMap<>(src));
            fv.setFormalityType(fpt.getType());
            fv.setDocumentType(fpt.getDocumentType());
            fv.setTrip(trip);
            fv.setManuallyAdded(false);
            fv.setSourceTemplate(fpt);
            trip.addFormality(fv);
        }
    }

    private TripFormalityDTO mapFormalityToDTO(TripFormality fv) {
        DocumentTypeDTO typeDocument = new DocumentTypeDTO(
                fv.getDocumentType().getId(),
                fv.getDocumentType().getAbr(),
                fv.getDocumentType().getLabel(),
                fv.getDocumentType().getDescription()
        );
        return new TripFormalityDTO(
                fv.getId(),
                typeDocument,
                fv.getFormalityType(),
                fv.isRequired(),
                fv.getDaysBeforeTrip(),
                fv.getAcceptedMime(),
                fv.getMaxSizeMb(),
                fv.getDaysRetentionAfterTrip(),
                fv.getStoreScan(),
                fv.getTripCondition(),
                fv.getNotes(),
                fv.isManuallyAdded()
        );
    }

    private TripDetailDTO mapToTripDTO(Trip trip) {
        DateRangeDTO registrationRange = new DateRangeDTO(
                trip.getRegistrationOpeningDate(),
                trip.getRegistrationClosingDate()
        );
        DateRangeDTO tripDates = new DateRangeDTO(
                trip.getDepartureDate(),
                trip.getReturnDate()
        );
        List<ChaperoneDTO> chaperones = trip.getChaperones().stream()
                .map(org -> new ChaperoneDTO(
                        org.getPublicId(),
                        org.getFirstName(),
                        org.getLastName(),
                        org.getEmail(),
                        org.getTelephone()
                ))
                .toList();
        List<SectionDTO> sections = trip.getSections().stream()
                .map(sectionMapper::toDTO)
                .toList();

        Long interestedCount = tripPreferenceService.countInterestedUsers(trip.getId());

        User currentUser = currentUserService.getCurrentUser();
        boolean interested = tripPreferenceService.isInterested(currentUser, trip.getId());

        CountryDTO paysDTO = new CountryDTO(trip.getCountry().getId(), trip.getCountry().getName());
        return new TripDetailDTO(
                trip.getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getDestination(),
                trip.getFamilyContribution(),
                trip.getCoverPhotoUrl(),
                paysDTO,
                tripDates,
                trip.getMinParticipants(),
                trip.getMaxParticipants(),
                registrationRange,
                trip.getPoll(),
                List.of(), // Formalités non incluses dans la liste paginée
                chaperones,
                interestedCount,
                sections,
                interested,
                trip.getUpdatedAt().toString()
        );
    }
}
