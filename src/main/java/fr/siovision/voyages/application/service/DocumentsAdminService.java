package fr.siovision.voyages.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.siovision.voyages.domain.model.StudentHealthForm;
import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.DocumentsAdminRepository;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import fr.siovision.voyages.infrastructure.repository.TripRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentsAdminService {
    private static final String HEALTH_FORM_CODE = "health_form";

    private final DocumentsAdminRepository repo;
    private final FileService fileService;
    private final UserRepository userRepo;
    private final StudentHealthFormRepository studentHealthFormRepository;
    private final TripRepository tripRepository;
    private final ObjectMapper objectMapper;


    @Transactional(readOnly = true)
    public DocumentsAdminDTO getUserDocumentsForTrip(UUID userPublicId, Long tripId) {
        // 1) Charger l'utilisateur et le voyage
        var user = userRepo.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur inconnu"));
        Long userId = user.getId();

        var trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Voyage inconnu"));

        var formalities = repo.listFormalitiesForTrip(tripId);
        var provided    = repo.findLatestProvidedPerType(userId, tripId);

        // 2) Fichiers fournis, par type
        Map<Long, DocumentsAdminRepository.ProvidedRow> byType = provided.stream()
                .collect(Collectors.toMap(
                        DocumentsAdminRepository.ProvidedRow::getDocumentTypeId,
                        p -> p
                ));

        // 3) Fiche sanitaire la plus récente pour cet utilisateur (s’il y en a une)
        var healthForms = studentHealthFormRepository
                .findAllSortedByUserAndRecencyByPublicIds(List.of(userPublicId));
        StudentHealthForm healthForm = healthForms.isEmpty() ? null : healthForms.getFirst();

        int requiredCount = 0;
        int providedCount = 0;

        List<DocumentItemAdminDTO> items = new ArrayList<>(formalities.size());

        final var departure = trip.getDepartureDate(); // LocalDate
        final boolean userIsMinor = isMinorAt(user.getBirthDate(), departure);

        for (var f : formalities) {
            boolean requiredForUser = isFormalityRequiredForUser(f, userIsMinor);
            if (requiredForUser) {
                requiredCount++;
            }

            boolean isHealthForm = HEALTH_FORM_CODE.equalsIgnoreCase(nz(f.getAbr()));
            boolean isProvided = false;
            Date providedAt = null;
            DocumentObjectMiniDTO lastObject = null;

            if (isHealthForm) {
                if (healthForm != null && requiredForUser) {
                    // Option : vérifier la validité (validUntil) comme dans TripRegistrationAdminService
                    isProvided = true;
                    providedAt = Date.from(healthForm.getCreatedAt());
                }
            } else {
                var p = byType.get(f.getDocumentTypeId());
                if (p != null && p.getPublicId() != null && p.getProvidedAt() != null) {
                    isProvided = true;
                    // LocalDateTime -> Date
                    var ldt = p.getProvidedAt();
                    providedAt = Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());

                    lastObject = new DocumentObjectMiniDTO(
                            p.getPublicId().toString(),
                            Optional.ofNullable(p.getSize()).orElse(0L),
                            nz(p.getMime()),
                            Optional.ofNullable(p.getPreviewable()).orElse(Boolean.FALSE)
                    );
                }
            }

            if (requiredForUser && isProvided) {
                providedCount++;
            }

            items.add(new DocumentItemAdminDTO(
                    new DocumentTypeAdminDTO(
                            f.getDocumentTypeId(),
                            nz(f.getAbr()),
                            nz(f.getLabel())
                    ),
                    requiredForUser,   // 🔴 surtout pas f.getRequired() brut
                    isProvided,
                    providedAt,
                    lastObject
            ));
        }

        int missing = Math.max(0, requiredCount - providedCount);

        return new DocumentsAdminDTO(
                userId,
                tripId,
                new DocumentsSummaryDTO(requiredCount, providedCount, missing),
                items
        );
    }

    @Transactional(readOnly = true)
    public PreviewUrlDTO getPreviewUrl(String docPublicId) {
        final UUID uuid;
        try {
            uuid = UUID.fromString(docPublicId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant de document invalide");
        }

        String objectKey = repo.findObjectKeyByPublicId(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable"));

        // Utilise la durée par défaut définie dans FileService (app.s3.presign-duration-min)
        String url = fileService.presignGet(objectKey);

        return new PreviewUrlDTO(url);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static boolean isMinorAt(LocalDate birthDate, LocalDate date) {
        if (birthDate == null || date == null) return false;
        // Mineur si la date du départ est STRICTEMENT avant le 18e anniversaire
        return date.isBefore(birthDate.plusYears(18));
    }


    private Map<String, Object> parseCondition(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Si tu préfères fail-fast :
            // throw new IllegalStateException("JSON invalide pour trip_condition: " + json, e);
            return Collections.emptyMap(); // pour l'instant : pas de condition = applicable à tous
        }
    }

    /**
     * Retourne true si la formalité f est requise pour un user donné (mineur/majeur).
     */
    private boolean isFormalityRequiredForUser(DocumentsAdminRepository.FormalityRow f, boolean userIsMinor) {
        if (!Boolean.TRUE.equals(f.getRequired())) {
            return false;
        }

        String condJson = f.getTripCondition();
        Map<String, Object> cond = parseCondition(condJson);

        if (cond.isEmpty()) {
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
                // Type exotique => par prudence, on considère que ce n'est PAS "mineur only"
                    false;
        };

        return requiredMinor == userIsMinor;
    }
}