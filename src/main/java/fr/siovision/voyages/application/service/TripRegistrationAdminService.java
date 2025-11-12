package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.StudentHealthForm;
import fr.siovision.voyages.domain.model.Trip;
import fr.siovision.voyages.domain.model.TripRegistrationStatus;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
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

        // 1) Compte base des formalités FICHIER requises pour le voyage
        int baseRequiredCount = tripDocumentsAdminRepository.countRequiredForTrip(tripId);

        // 2) On travaille en publicId (UUID), pas en Long
        var userPublicIds = page.getContent().stream()
                .map(TripUserRepository.TripRegistrationRow::getUserPublicId) // UUID
                .distinct()
                .toList();

        // 3) Comptage des "fournis" par utilisateur (clé = publicId)
        var providedByUser = tripDocumentsAdminRepository
                .countProvidedByUserPublicIdsForTrip(tripId, userPublicIds).stream()
                .collect(Collectors.toMap(
                        TripDocumentsAdminRepository.ProvidedCountRow::getUserPublicId,
                        TripDocumentsAdminRepository.ProvidedCountRow::getProvidedCount
                ));

        // 4) Chargement du trip (formalité health_form potentielle)
        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage inconnu"));

        var formalities = trip.getFormalities();
        boolean hasHealthFormFormality = formalities != null && formalities.stream()
                .anyMatch(f -> f.getDocumentType() != null
                        && HEALTH_FORM_CODE.equalsIgnoreCase(f.getDocumentType().getAbr())
                        && Boolean.TRUE.equals(f.isRequired()));

        if (!hasHealthFormFormality) {
            // Pas de fiche sanitaire requise → comportement inchangé
            return page.map(r -> {
                int provided = providedByUser.getOrDefault(r.getUserPublicId(), 0);
                int missing  = Math.max(0, baseRequiredCount - provided);
                var summary  = new DocumentsSummaryDTO(baseRequiredCount, provided, missing);
                return mapRowToViewDTO(r, summary);
            });
        }

        // 5) Batch-load Users (clé = publicId) pour évaluer les conditions (ex: minorité)
        var usersByPublicId = userRepository.findAllByPublicIdIn(userPublicIds).stream()
                .collect(Collectors.toMap(User::getPublicId, u -> u));

        // 6) Batch-load des fiches sanitaires les plus récentes (clé = publicId)
        var latestForms = collapseLatestByUser(
                studentHealthFormRepository.findAllSortedByUserAndRecencyByPublicIds(userPublicIds)
        ); // cf. signature proposée juste après

        final var now = java.time.Instant.now();
        final var departure = trip.getDepartureDate(); // LocalDate

        java.util.function.BiFunction<User, Trip, Boolean> healthRequiredForUser = (user, t) -> {
            if (formalities == null) return false;
            for (var f : formalities) {
                if (f.getDocumentType() == null) continue;
                if (!HEALTH_FORM_CODE.equalsIgnoreCase(f.getDocumentType().getAbr())) continue;
                if (!Boolean.TRUE.equals(f.isRequired())) continue;

                var cond = f.getTripCondition();
                if (cond == null || cond.isEmpty()) return true;

                Object minor = cond.get("student.is_minor");
                if (minor != null) {
                    boolean requiredMinor = Boolean.TRUE.equals(minor);
                    boolean userIsMinor = isMinorAt(user.getBirthDate(), departure);
                    if (requiredMinor != userIsMinor) continue; // cette formalité ne s'applique pas
                    return true;
                }
                return true;
            }
            return false;
        };

        return page.map(r -> {
            UUID pid = r.getUserPublicId();

            int providedFiles = providedByUser.getOrDefault(pid, 0);

            // Fiche sanitaire requise pour cet utilisateur ?
            var user = usersByPublicId.get(pid);
            boolean hfRequired = (user != null) && healthRequiredForUser.apply(user, trip);

            int required = baseRequiredCount + (hfRequired ? 1 : 0);

            // Fiche sanitaire fournie & valide ?
            int provided = providedFiles;
            if (hfRequired) {
                var f = latestForms.get(pid);
                boolean present = (f != null);
                boolean valid = false;
                if (present) {
                    var expiryState = expiryStatus(f.getValidUntil(), now);
                    valid = (expiryState != Expiry.EXPIRED);
                }
                if (present && valid) {
                    provided += 1;
                }
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