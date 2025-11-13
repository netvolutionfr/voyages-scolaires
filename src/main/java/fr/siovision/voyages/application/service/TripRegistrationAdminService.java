package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripRegistrationAdminService {
    private final TripUserRepository tripUserRepository;
    private final UserRepository userRepository;
    private final TripDocumentsAdminRepository tripDocumentsAdminRepository;
    private final TripRepository tripRepository;
    private final StudentHealthFormRepository studentHealthFormRepository;

    private static final String HEALTH_FORM_CODE = "health_form";

    @Transactional
    public RegistrationAdminUpdateResponse updateStatus(Long tripUserId, RegistrationAdminUpdateRequest req) {
        var tu = tripUserRepository.findById(tripUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription inconnue"));

        tu.setRegistrationStatus(req.status());
        tu.setDecisionDate(LocalDateTime.now());
        tu.setDecisionMessage(req.adminMessage());
        tripUserRepository.save(tu);

        return new RegistrationAdminUpdateResponse(
                tu.getId(),
                tu.getTrip().getId(),
                tu.getUser().getId(),
                tu.getRegistrationStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public Page<TripRegistrationAdminViewDTO> listTripRegistrations(
            Long tripId, String status, String q, Long sectionId,
            boolean includeDocSummary, Pageable pageable
    ) {
        Integer statusCode = toStatusCode(status);

        Page<TripUserRepository.TripRegistrationRow> page =
                tripUserRepository.searchAdminRegistrations(tripId, statusCode, nullIfBlank(q), sectionId, pageable);

        if (!includeDocSummary || page.isEmpty()) {
            return page.map(this::mapRowToViewDTO);
        }

        // 1) On travaille en publicId (UUID), pas en Long
        var userPublicIds = page.getContent().stream()
                .map(TripUserRepository.TripRegistrationRow::getUserPublicId)
                .distinct()
                .toList();

        // 2) Comptage des "fournis" FICHIER par utilisateur (clé = publicId)
        var providedByUser = tripDocumentsAdminRepository
                .countProvidedByUserPublicIdsForTrip(tripId, userPublicIds).stream()
                .collect(Collectors.toMap(
                        TripDocumentsAdminRepository.ProvidedCountRow::getUserPublicId,
                        TripDocumentsAdminRepository.ProvidedCountRow::getProvidedCount
                ));

        // 3) Chargement du trip + formalités
        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage inconnu"));

        var formalities = trip.getFormalities();

        // 4) Batch-load Users (clé = publicId) pour évaluer les conditions (ex: minorité)
        var usersByPublicId = userRepository.findAllByPublicIdIn(userPublicIds).stream()
                .collect(Collectors.toMap(User::getPublicId, u -> u));

        // 5) Batch-load des fiches sanitaires les plus récentes (clé = publicId)
        var latestForms = collapseLatestByUser(
                studentHealthFormRepository.findAllSortedByUserAndRecencyByPublicIds(userPublicIds)
        );

        final var now = java.time.Instant.now();
        final var departure = trip.getDepartureDate(); // LocalDate

        return page.map(r -> {
            UUID pid = r.getUserPublicId();
            var user = usersByPublicId.get(pid);

            if (formalities == null || user == null) {
                int provided = providedByUser.getOrDefault(pid, 0);
                var summary = new DocumentsSummaryDTO(0, provided, 0);
                return mapRowToViewDTO(r, summary);
            }

            boolean userIsMinor = isMinorAt(user.getBirthDate(), departure);

            // a) Nombre de formalités FICHIER requises pour CE user
            long requiredFilesForUser = formalities.stream()
                    .filter(f -> f.getDocumentType() != null)
                    .filter(f -> !HEALTH_FORM_CODE.equalsIgnoreCase(f.getDocumentType().getAbr()))
                    .filter(f -> isFormalityRequiredForUser(f, userIsMinor))
                    .count();

            // b) Nombre de formalités health_form requises pour CE user
            long requiredHealthFormsForUser = formalities.stream()
                    .filter(f -> f.getDocumentType() != null)
                    .filter(f -> HEALTH_FORM_CODE.equalsIgnoreCase(f.getDocumentType().getAbr()))
                    .filter(f -> isFormalityRequiredForUser(f, userIsMinor))
                    .count();

            int required = (int) (requiredFilesForUser + requiredHealthFormsForUser);

            // c) Fichiers fournis
            int provided = providedByUser.getOrDefault(pid, 0);

            // d) Fiche sanitaire fournie & valide (si requise pour ce user)
            if (requiredHealthFormsForUser > 0) {
                var form = latestForms.get(pid);
                boolean present = (form != null);
                boolean valid = false;
                if (present) {
                    var expiryState = expiryStatus(form.getValidUntil(), now);
                    valid = (expiryState != Expiry.EXPIRED);
                }
                if (present && valid) {
                    provided += 1;
                }
            }

            // éviter d'avoir plus de "fourni" que de "requis"
            if (provided > required) {
                provided = required;
            }

            int missing = Math.max(0, required - provided);
            var summary = new DocumentsSummaryDTO(required, provided, missing);

            return mapRowToViewDTO(r, summary);
        });
    }

    @Transactional(readOnly = true)
    public RegistrationAdminDetailDTO getRegistrationDetail(Long registrationId) {
        var row = tripUserRepository.findAdminRegistrationRow(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription inconnue"));

        // En DÉTAIL, on peut hydrater l'entité User pour récupérer téléphone déchiffré (si tu le souhaites)
        var user = userRepository.findById(row.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        var section = (row.getSectionId() != null)
                ? new SectionMiniDTO(row.getSectionId(), nz(row.getSectionLabel()))
                : null;

        var userDto = new UserMiniDTO(
                row.getUserPublicId(),
                nz(row.getFirstName()),
                nz(row.getLastName()),
                nz(row.getEmail()),
                nz(user.getTelephone()), // déchiffré via @Convert en entité
                section
        );

        return new RegistrationAdminDetailDTO(
                row.getRegistrationId(),
                new TripMiniDTO(row.getTripId(), nz(row.getTripTitle())),
                userDto,
                nz(row.getStatus()),
                row.getRegisteredAt()
        );
    }

    /* ---------------- mapping helpers ---------------- */

    private TripRegistrationAdminViewDTO mapRowToViewDTO(TripUserRepository.TripRegistrationRow r) {
        return mapRowToViewDTO(r, null);
    }

    private TripRegistrationAdminViewDTO mapRowToViewDTO(TripUserRepository.TripRegistrationRow r,
                                                         DocumentsSummaryDTO summaryOrNull) {
        var section = (r.getSectionId() != null)
                ? new SectionMiniDTO(r.getSectionId(), nz(r.getSectionLabel()))
                : null;

        // LISTE : pas de téléphone (pas d’accès aux données chiffrées ici)
        var userMini = new UserMiniDTO(
                r.getUserPublicId(),
                nz(r.getFirstName()),
                nz(r.getLastName()),
                nz(r.getEmail()),
                null, // <- téléphone absent en liste
                section
        );

        return new TripRegistrationAdminViewDTO(
                r.getRegistrationId(),
                r.getRegisteredAt(),
                nz(r.getStatus()),
                userMini,
                summaryOrNull
        );
    }

    private Integer toStatusCode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Integer.valueOf(raw); } catch (NumberFormatException ignored) {}
        try { return TripRegistrationStatus.valueOf(raw.trim().toUpperCase()).ordinal(); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static String nullIfBlank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String nz(String s) { return s == null ? "" : s; }

    private boolean isFormalityRequiredForUser(TripFormality f, boolean userIsMinor) {
        if (!f.isRequired()) {
            return false;
        }

        Map<String, Object> cond = f.getTripCondition();
        if (cond == null || cond.isEmpty()) {
            // Aucune condition => requis pour tous
            return true;
        }

        Object minor = cond.get("student.is_minor");
        if (minor == null) {
            // Pas de condition sur la minorité => requis pour tous
            return true;
        }

        boolean requiredMinor = switch (minor) {
            case Boolean b -> b;
            case String s -> Boolean.parseBoolean(s);
            case Number n -> n.intValue() != 0;
            default ->
                // Type exotique : par prudence, on considère "non mineur only"
                    false;
        };

        return requiredMinor == userIsMinor;
    }

    private static boolean isMinorAt(java.time.LocalDate birthDate, java.time.LocalDate onDate) {
        if (birthDate == null || onDate == null) return false;
        return java.time.Period.between(birthDate, onDate).getYears() < 18;
    }

    private enum Expiry { VALID, EXPIRES_SOON, EXPIRED }
    private static Expiry expiryStatus(java.time.Instant validUntil, java.time.Instant now) {
        if (validUntil == null) return Expiry.VALID;
        if (validUntil.isBefore(now)) return Expiry.EXPIRED;
        // adjust threshold if you want to use it somewhere later
        if (validUntil.isBefore(now.plusSeconds(30L * 24 * 3600))) return Expiry.EXPIRES_SOON;
        return Expiry.VALID;
    }

    // Keep only the first/latest per user (list is already sorted by repo query)
    private static Map<UUID, StudentHealthForm> collapseLatestByUser(List<StudentHealthForm> rows) {
        var map = new LinkedHashMap<UUID, StudentHealthForm>();
        for (var f : rows) {
            UUID pid = f.getStudent().getPublicId();
            if (!map.containsKey(pid)) {
                map.put(pid, f);
            }
        }
        return map;
    }
}