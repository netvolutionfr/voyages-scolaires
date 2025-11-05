package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.*;
import fr.siovision.voyages.infrastructure.repository.DocumentsAdminRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentsAdminService {

    private final DocumentsAdminRepository repo;
    private final FileService fileService;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public DocumentsAdminDTO getUserDocumentsForTrip(UUID userPublicId, Long tripId) {
        Long userId = userRepo.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur inconnu"))
                .getId();
        var formalities = repo.listFormalitiesForTrip(tripId);
        var provided    = repo.findLatestProvidedPerType(userId, tripId);

        Map<Long, DocumentsAdminRepository.ProvidedRow> byType = provided.stream()
                .collect(Collectors.toMap(DocumentsAdminRepository.ProvidedRow::getDocumentTypeId, p -> p));

        int requiredCount = 0;
        int providedCount = 0;

        List<DocumentItemAdminDTO> items = new ArrayList<>(formalities.size());
        for (var f : formalities) {
            boolean required = Boolean.TRUE.equals(f.getRequired());
            if (required) requiredCount++;

            var p = byType.get(f.getDocumentTypeId());
            boolean isProvided = p != null
                    && p.getPublicId() != null
                    && p.getProvidedAt() != null;

            if (required && isProvided) providedCount++;

            DocumentObjectMiniDTO lastObject = (p != null && p.getPublicId() != null)
                    ? new DocumentObjectMiniDTO(
                    p.getPublicId().toString(),
                    Optional.ofNullable(p.getSize()).orElse(0L),
                    nz(p.getMime()),
                    Optional.ofNullable(p.getPreviewable()).orElse(Boolean.FALSE)
            )
                    : null;

            items.add(new DocumentItemAdminDTO(
                    new DocumentTypeAdminDTO(
                            f.getDocumentTypeId(),
                            nz(f.getAbr()),
                            nz(f.getLabel())
                    ),
                    required,
                    isProvided,
                    p != null ? p.getProvidedAt() : null,
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
}