package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.DocumentRepository;
import fr.siovision.voyages.infrastructure.repository.DocumentTypeRepository;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import fr.siovision.voyages.infrastructure.repository.TripUserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;


@Service
@RequiredArgsConstructor
public class DocumentsService {

    private final CurrentUserService currentUserService;
    private final TripUserRepository tripUserRepository;
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final StudentHealthFormRepository studentHealthFormRepository;

    private static final String HEALTH_FORM_CODE = "health_form";

    public DocumentsDTO list() {
        final User user = currentUserService.getCurrentUser();
        final LocalDate today = LocalDate.now();
        final Instant now = Instant.now();
        final HealthFormSnapshot health = loadHealthForm(user.getId());

        // 1) TripUser → Voyages "actifs" (inscrit/accepté + à venir)
        final List<TripUser> links = tripUserRepository.findActiveByUser(
                user.getId(),
                activeStatuses(), // CONFIRMED/ENROLLED etc.
                today
        );

        // Liste distincte des trips + résumé pour payload
        final List<Trip> trips = links.stream()
                .map(TripUser::getTrip)
                .distinct()
                .toList();

        final List<TripSummaryDTO> tripSummaries = trips.stream()
                .map(t -> new TripSummaryDTO(t.getId(), t.getTitle(), t.getCountry().getId()))
                .toList();

        // 2) Dernier document READY par documentType pour cet utilisateur
        final Map<Long, Document> providedByType = latestReadyByType(user.getId());

        // 3) Agréger toutes les formalités de tous les trips par documentType
        final Map<Long, Agg> agg = new LinkedHashMap<>();

        for (Trip t : trips) {
            for (TripFormality f : t.getFormalities()) {
                final DocumentType dt = f.getDocumentType();
                final long typeId = dt.getId();

                Agg a = agg.computeIfAbsent(typeId, id -> Agg.fromFormality(f));
                a.mergeFormality(f);

                // Conditions (ex: mineur)
                if (f.isRequired() && conditionsSatisfied(f, user, t)) {
                    a.required = true;
                    a.requiredByTrips.add(new RequiredByTripsDTO(t.getId(), t.getTitle()));
                    a.requiredTripIds.add(t.getId());
                }
            }
        }


        // 4) Injecter les types de scope=GENERAL même si aucun trip
        final List<DocumentType> generalTypes = documentTypeRepository.findByScope(DocumentScope.GENERAL);

        for (DocumentType dt : generalTypes) {
            final long typeId = dt.getId();
            if (!agg.containsKey(typeId)) {
                Agg a = new Agg();
                a.documentTypeId = typeId;
                a.code  = dt.getAbr();     // ex: "health_form"
                a.label = dt.getLabel();

                // Par défaut, beaucoup de GENERAL sont des fichiers,
                // MAIS la fiche sanitaire est un FORM.
                if (HEALTH_FORM_CODE.equalsIgnoreCase(a.code)) {
                    a.kind = "FORM";
                    a.acceptedMime = new LinkedHashSet<>(); // aucun fichier attendu
                    a.maxSizeMb = null;
                } else {
                    a.kind  = "FILE";
                    a.acceptedMime = new LinkedHashSet<>(List.of("application/pdf","image/jpeg","image/png"));
                    a.maxSizeMb = 10;
                }

                a.required = false; // jamais requis par défaut tant qu’aucun voyage ne le demande
                agg.put(typeId, a);
            }
        }

        // 5) Construction des items + missing (les GENERAL ne comptent pas dans "required")
        final List<DocumentItemDTO> items = new ArrayList<>();
        int totalRequired = 0, totalMissing = 0;
        final Map<Long, Integer> missingByTrip = new HashMap<>();

        for (Agg a : agg.values()) {
            final Document doc = providedByType.get(a.documentTypeId);

            final String scope = generalTypes.stream().anyMatch(gt -> gt.getId().equals(a.documentTypeId))
                    ? "GENERAL" : "TRIP";

            boolean provided = false;
            Optional<Instant> providedAt = Optional.empty();
            Optional<DocumentObjectDTO> lastObject = Optional.empty();
            List<DocumentWarningDTO> warnings = new ArrayList<>();

            if (HEALTH_FORM_CODE.equalsIgnoreCase(a.code)) {
                // ===== FICHE SANITAIRE =====
                provided = health.present();

                if (health.providedAt() != null) {
                    providedAt = Optional.of(health.providedAt());
                }

                final Expiry ex = expiryStatus(health.validUntil(), now);
                if (a.required) {
                    if (ex == Expiry.EXPIRED) {
                        provided = false;
                        warnings.add(new DocumentWarningDTO(
                                "HEALTH_FORM_EXPIRED",
                                "La fiche sanitaire est expirée."
                        ));
                    } else if (ex == Expiry.EXPIRES_SOON) {
                        warnings.add(new DocumentWarningDTO(
                                "HEALTH_FORM_EXPIRES_SOON",
                                "La fiche sanitaire arrive bientôt à expiration."
                        ));
                    }
                } else {
                    if (ex == Expiry.EXPIRES_SOON) {
                        warnings.add(new DocumentWarningDTO(
                                "HEALTH_FORM_EXPIRES_SOON",
                                "La fiche sanitaire arrive bientôt à expiration."
                        ));
                    }
                }

            } else {
                // ===== DOCUMENT FICHIER =====
                if (doc != null) {
                    provided = true;
                    providedAt = Optional.ofNullable(doc.getCreatedAt())
                            .map(dt2 -> dt2.atZone(ZoneId.systemDefault()).toInstant());

                    lastObject = Optional.of(new DocumentObjectDTO(
                            doc.getPublicId().toString(),
                            doc.getSize(),
                            doc.getMime(),
                            doc.getSha256(),
                            doc.getDocumentStatus().name(), // ou null si pas utilisé
                            isPreviewable(doc.getMime())
                    ));
                }
            }

            // Comptage “required/missing” (les GENERAL non requis ne comptent pas)
            if (a.required) {
                totalRequired++;
                if (!provided) {
                    totalMissing++;
                    for (Long tripId : a.requiredTripIds) {
                        missingByTrip.merge(tripId, 1, Integer::sum);
                    }
                }
            }

            final DocumentTypeDetailDTO typeDTO = new DocumentTypeDetailDTO(
                    a.documentTypeId,
                    a.code,
                    a.label,
                    a.kind,
                    a.acceptedMime.toArray(new String[0]),
                    a.maxSizeMb,
                    scope
            );

            items.add(new DocumentItemDTO(
                    typeDTO,
                    a.required,
                    List.copyOf(a.requiredByTrips),
                    provided,
                    providedAt,
                    lastObject,
                    List.copyOf(warnings)
            ));
        }

        final List<TripMissingDocumentDTO> byTrip = trips.stream()
                .map(t -> new TripMissingDocumentDTO(
                        t.getId().intValue(),
                        missingByTrip.getOrDefault(t.getId(), 0)
                ))
                .toList();

        final MissingDocumentDTO missing = new MissingDocumentDTO(
                totalRequired,
                totalMissing,
                byTrip
        );

        return new DocumentsDTO(
                user.getId(),
                tripSummaries,
                items,
                missing
        );
    }

    // ---- Helpers -------------------------------------------------------------

    private Collection<TripRegistrationStatus> activeStatuses() {
        // Adapte selon ton enum (exemples)
        return List.of(TripRegistrationStatus.ENROLLED, TripRegistrationStatus.CONFIRMED);
    }

    private Map<Long, Document> latestReadyByType(Long userId) {
        List<Document> list = documentRepository.findAllReadyByUser(userId);
        Map<Long, Document> map = new HashMap<>();
        for (Document d : list) {
            long typeId = d.getDocumentType().getId();
            map.putIfAbsent(typeId, d); // liste triée desc par createdAt → on garde le 1er
        }
        return map;
    }

    private boolean conditionsSatisfied(TripFormality f, User user, Trip trip) {
        Map<String, Object> cond = f.getTripCondition();
        if (cond == null || cond.isEmpty()) return true;

        Object minor = cond.get("student.is_minor");
        if (minor != null) {
            boolean requiredMinor = Boolean.TRUE.equals(minor);
            boolean userIsMinor = isMinorAt(user.getBirthDate(), trip.getDepartureDate());
            return requiredMinor == userIsMinor;
        }
        // (ajouter d’autres règles : pays, nationalité… si nécessaire)
        return true;
    }

    private boolean isMinorAt(LocalDate birthDate, LocalDate onDate) {
        if (birthDate == null || onDate == null) return false;
        return Period.between(birthDate, onDate).getYears() < 18;
    }

    private boolean isPreviewable(String mime) {
        if (mime == null) return false;
        return mime.startsWith("image/") || "application/pdf".equals(mime);
    }

    @Getter @Setter
    private static class Agg {
        Long documentTypeId;
        String code;                     // ← DocumentType.abr
        String label;
        String kind;                     // "FILE" | "FORM"
        Set<String> acceptedMime = new LinkedHashSet<>();
        Integer maxSizeMb;
        boolean required = false;
        List<RequiredByTripsDTO> requiredByTrips = new ArrayList<>();
        Set<Long> requiredTripIds = new HashSet<>();

        static Agg fromFormality(TripFormality f) {
            Agg a = new Agg();
            a.documentTypeId = f.getDocumentType().getId();
            a.code = f.getDocumentType().getAbr();
            a.label = f.getDocumentType().getLabel();
            a.kind = f.getFormalityType().name();
            a.mergeFormality(f);
            return a;
        }

        void mergeFormality(TripFormality f) {
            if (f.getAcceptedMime() != null) this.acceptedMime.addAll(f.getAcceptedMime());
            if (f.getMaxSizeMb() != null) {
                if (this.maxSizeMb == null) this.maxSizeMb = f.getMaxSizeMb();
                else this.maxSizeMb = Math.min(this.maxSizeMb, f.getMaxSizeMb());
            }
        }
    }

    private record HealthFormSnapshot(
            boolean present,
            Instant providedAt,   // signé ou, à défaut, mis à jour
            Instant validUntil    // peut être null
    ) {}

    private HealthFormSnapshot loadHealthForm(Long userId) {
        return studentHealthFormRepository.findLatestByUserId(userId)
                .map(f -> new HealthFormSnapshot(
                        true,
                        f.getSignedAt() != null ? f.getSignedAt() : f.getUpdatedAt(),
                        f.getValidUntil()
                ))
                .orElseGet(() -> new HealthFormSnapshot(false, null, null));
    }

    private enum Expiry { VALID, EXPIRES_SOON, EXPIRED }

    private Expiry expiryStatus(Instant validUntil, Instant now) {
        if (validUntil == null) return Expiry.VALID;
        if (validUntil.isBefore(now)) return Expiry.EXPIRED;
        // “bientôt” = moins de 30 jours
        if (validUntil.isBefore(now.plusSeconds(30L * 24 * 3600))) return Expiry.EXPIRES_SOON;
        return Expiry.VALID;
    }
}