package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.CurrentUserService;
import fr.siovision.voyages.application.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final CurrentUserService currentUserService; // pour l'id user

    @GetMapping("/presign")
    public Map<String, String> presign(
            @RequestParam String filename,
            @RequestParam(defaultValue = "image/jpeg") String contentType,
            @RequestParam(defaultValue = "cover") String mode,
            @RequestParam(required = false) String docCode
    ) {
        // Flux "cover"
        if ("cover".equalsIgnoreCase(mode)) {
            if (!isAllowedForCover(contentType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType non autorisé pour une couverture");
            }
            String key = fileService.buildCoverKey(filename);
            return fileService.presignPut(key, contentType);
        }

        // Flux "document"
        if ("document".equalsIgnoreCase(mode)) {
            if (!isAllowedForDocument(contentType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType non autorisé pour un document");
            }
            var user = currentUserService.getCurrentUser();
            String key = fileService.buildDocumentKey(user.getId(), docCode, filename);
            return fileService.presignPut(key, contentType);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode invalide (cover|document attendu)");
    }

    private boolean isAllowedForCover(String ct) {
        return "image/jpeg".equals(ct) || "image/png".equals(ct) || "image/webp".equals(ct);
    }

    private boolean isAllowedForDocument(String ct) {
        return "application/pdf".equals(ct) || "image/jpeg".equals(ct) || "image/png".equals(ct);
    }
}
