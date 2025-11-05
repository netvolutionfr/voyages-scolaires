package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.TripRegistrationStatus;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.TripDocumentsAdminRepository;
import fr.siovision.voyages.infrastructure.repository.TripUserRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TripRegistrationAdminService {
    private final TripUserRepository tripUserRepository;
    private final UserRepository userRepository;
    private final TripDocumentsAdminRepository tripDocumentsAdminRepository;

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
            // OK maintenant : méthode 1-arg existe
            return page.map(this::mapRowToViewDTO);
        }

        int requiredCount = tripDocumentsAdminRepository.countRequiredForTrip(tripId);

        var userIds = page.getContent().stream()
                .map(TripUserRepository.TripRegistrationRow::getUserId)
                .distinct()
                .toList();

        var providedByUser = tripDocumentsAdminRepository
                .countProvidedByUserForTrip(tripId, userIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        TripDocumentsAdminRepository.ProvidedCountRow::getUserId,
                        TripDocumentsAdminRepository.ProvidedCountRow::getProvidedCount
                ));

        return page.map(r -> {
            int provided = providedByUser.getOrDefault(r.getUserId(), 0);
            int missing  = Math.max(0, requiredCount - provided);
            var summary  = new DocumentsSummaryDTO(requiredCount, provided, missing);

            // ✅ plus d'appel à une méthode absente ; on réutilise le mapper central
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
}