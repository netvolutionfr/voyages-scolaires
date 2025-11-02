package fr.siovision.voyages.application.service;

import fr.siovision.voyages.application.security.EncryptionService;
import fr.siovision.voyages.domain.events.DocumentStorageDeletionEvent;
import fr.siovision.voyages.domain.model.Document;
import fr.siovision.voyages.domain.model.DocumentType;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.DocumentRepository;
import fr.siovision.voyages.infrastructure.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentUploadService {
    private final CurrentUserService currentUserService;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final EncryptionService encryption;
    private final ApplicationEventPublisher events;

    @Transactional
    public Document uploadAndReplace(Long documentTypeId, byte[] bytes, String originalName, String mime) throws Exception {
        final User user = currentUserService.getCurrentUser();

        final DocumentType dt = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentType inconnu"));

        if (bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }

        // Récupérer l’ancien AVANT insertion
        final Optional<Document> previousOpt =
                documentRepository.findFirstByUserIdAndDocumentTypeIdOrderByCreatedAtDesc(user.getId(), dt.getId());

        // Upload chiffré → S3
        final String objectKey = storageService.buildEncryptedKey(originalName);
        StorageService.EncryptedUploadResult res;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            res = storageService.putEncrypted(objectKey, in, mime);
        }

        // Persister le nouveau
        final String sha256 = encryption.computeSha256(new ByteArrayInputStream(bytes));
        final Document newDoc = new Document();
        newDoc.setPublicId(UUID.randomUUID());
        newDoc.setOriginalFilename(originalName);
        newDoc.setMime(mime);
        newDoc.setSize((long) bytes.length);
        newDoc.setSha256(sha256);
        newDoc.setObjectKey(objectKey);
        newDoc.setDocumentType(dt);
        newDoc.setDocumentStatus(Document.DocumentStatus.READY);
        newDoc.setUser(user);
        newDoc.setCreatedAt(LocalDateTime.now());
        newDoc.setUpdatedAt(LocalDateTime.now());

        newDoc.setIv(res.ivB64());
        newDoc.setDekWrapped(res.dekWrappedB64());
        newDoc.setDekIv(null);

        documentRepository.saveAndFlush(newDoc);

        // Supprimer l'ancien en base et publier un évènement pour S3 après commit
        previousOpt.ifPresent(old -> {
            final String oldKey = old.getObjectKey();
            documentRepository.delete(old);
            events.publishEvent(new DocumentStorageDeletionEvent(oldKey));
        });

        return newDoc;
    }
}
