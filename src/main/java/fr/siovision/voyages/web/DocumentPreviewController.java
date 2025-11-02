package fr.siovision.voyages.web;

import org.springframework.web.bind.annotation.RestController;
import fr.siovision.voyages.application.service.CurrentUserService;
import fr.siovision.voyages.application.service.FileService;
import fr.siovision.voyages.infrastructure.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/documents")
@RequiredArgsConstructor
public class DocumentPreviewController {

    private final CurrentUserService currentUserService;
    private final DocumentRepository documentRepository;
    private final FileService fileService;


    @GetMapping("/preview-url")
    public Map<String, String> previewUrl(@RequestParam("docId") UUID docPublicId) {
        var user = currentUserService.getCurrentUser();

        var doc = documentRepository.findOwned(docPublicId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Document introuvable"));

        // Option : limiter aux formats prévisualisables
        var mime = doc.getMime() == null ? "" : doc.getMime();
        if (!(mime.startsWith("image/") || "application/pdf".equals(mime))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aperçu indisponible pour ce type");
        }

        String url = fileService.presignGet(doc.getObjectKey()); // ← clé S3 depuis Document.objectKey
        return Map.of("url", url);
    }
}