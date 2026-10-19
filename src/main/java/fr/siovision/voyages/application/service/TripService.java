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
    private final TripUserRepository tripUserRepository;

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
        trip.setTransitCountries(resolveTransitCountries(tripUpsertRequest.getTransitCountryIds(), country));

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
        syncFormalitiesFromTemplates(trip);

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
        updated.setTransitCountries(resolveTransitCountries(tripUpsertRequest.getTransitCountryIds(), country));

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

        // Destination ou escales modifiées : formalités issues des templates resynchronisées,
        // formalités ajoutées manuellement conservées.
        syncFormalitiesFromTemplates(updated);

        log.info("Trip updated id={}", updated.getId());
        return tripRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public Page<TripDetailDTO> list(Pageable pageable) {
        // if student, only return trips with sections matching student's sections
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == UserRole.STUDENT) {
            Section userSection = currentUser.getSection();
            return tripRepository.findBySectionsContaining(userSection, pageable)
                    .map(this::mapToTripDTO);
        }
        if (currentUser.getRole() == UserRole.TEACHER) {
            return tripRepository.findByChaperonesContaining(currentUser, pageable)
                    .map(this::mapToTripDTO);
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return tripRepository.findAll(pageable).map(this::mapToTripDTO);
        }
        // for other roles (e.g., PARENT), return no trips
        return Page.empty(pageable);
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
                        org.getLastName(),
                        org.getFirstName(),
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

        // Vérifier que l'utilisateur est inscrit à ce voyage
        // Dans TripUsers, il y a une entrée si l'utilisateur est inscrit mais ce n'est pas dans tripPreference
        // Utiliser le TripUserRepository pour vérifier l'inscription
        boolean registered = tripUserRepository.existsByTrip_IdAndUser_Id(trip.getId(), currentUser.getId());

        return new TripDetailDTO(
                trip.getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getDestination(),
                trip.getFamilyContribution(),
                trip.getCoverPhotoUrl(),
                new CountryDTO(trip.getCountry().getId(), trip.getCountry().getName()),
                toCountryDTOs(trip.getTransitCountries()),
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
                registered,
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

    private List<Country> resolveTransitCountries(List<Long> ids, Country destination) {
        List<Country> transits = new ArrayList<>();
        if (ids == null) {
            return transits;
        }
        for (Long countryId : new LinkedHashSet<>(ids)) {
            if (countryId == null || countryId.equals(destination.getId())) {
                continue;
            }
            transits.add(countryRepository.findById(countryId)
                    .orElseThrow(() -> new EntityNotFoundException("Country not found id=" + countryId)));
        }
        return transits;
    }

    /**
     * Templates applicables : tous ceux de la destination, puis ceux des escales marqués appliesInTransit.
     * Un seul template par type de document (la destination est prioritaire).
     */
    static List<CountryFormalityTemplate> applicableTemplates(Country destination, List<Country> transits) {
        Map<Long, CountryFormalityTemplate> byDocumentType = new LinkedHashMap<>();
        destination.getCountryFormalities()
                .forEach(t -> byDocumentType.putIfAbsent(t.getDocumentType().getId(), t));
        // ponytail: un template par type de document ; deux conditions différentes pour un même type
        // (ex. RU : CNI si <19 ans) ne garde que la première, à revoir si un cas réel l'exige.
        for (Country transit : transits) {
            transit.getCountryFormalities().stream()
                    .filter(CountryFormalityTemplate::isAppliesInTransit)
                    .forEach(t -> byDocumentType.putIfAbsent(t.getDocumentType().getId(), t));
        }
        return new ArrayList<>(byDocumentType.values());
    }

    /**
     * Aligne les formalités du voyage sur ses pays (destination + escales).
     * Les formalités ajoutées manuellement ne sont jamais touchées. Les documents élèves
     * sont liés au type de document, pas à la formalité : supprimer une formalité ne perd aucun fichier.
     */
    static void syncFormalitiesFromTemplates(Trip trip) {
        List<CountryFormalityTemplate> templates = applicableTemplates(trip.getCountry(), trip.getTransitCountries());
        // Comparaison par id : equals() des entités n'inclut aucun champ.
        Set<Long> templateIds = new HashSet<>();
        templates.forEach(t -> templateIds.add(t.getId()));

        trip.getFormalities().removeIf(f -> !f.isManuallyAdded()
                && (f.getSourceTemplate() == null || !templateIds.contains(f.getSourceTemplate().getId())));

        Set<Long> coveredDocumentTypes = new HashSet<>();
        trip.getFormalities().forEach(f -> coveredDocumentTypes.add(f.getDocumentType().getId()));
        for (CountryFormalityTemplate fpt : templates) {
            if (coveredDocumentTypes.add(fpt.getDocumentType().getId())) {
                trip.addFormality(cloneFormality(fpt));
            }
        }
    }

    private static TripFormality cloneFormality(CountryFormalityTemplate fpt) {
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
        fv.setManuallyAdded(false);
        fv.setSourceTemplate(fpt);
        return fv;
    }

    private static List<CountryDTO> toCountryDTOs(List<Country> countries) {
        return countries.stream().map(c -> new CountryDTO(c.getId(), c.getName())).toList();
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
                        org.getLastName(),
                        org.getFirstName(),
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

        boolean registered = tripUserRepository.existsByTrip_IdAndUser_Id(trip.getId(), currentUser.getId());

        CountryDTO paysDTO = new CountryDTO(trip.getCountry().getId(), trip.getCountry().getName());
        return new TripDetailDTO(
                trip.getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getDestination(),
                trip.getFamilyContribution(),
                trip.getCoverPhotoUrl(),
                paysDTO,
                toCountryDTOs(trip.getTransitCountries()),
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
                registered,
                trip.getUpdatedAt().toString()
        );
    }

    public void deleteTrip(Long id) {
        // TODO
    }
}
