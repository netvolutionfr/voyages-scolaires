package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.DocumentUploadService;
import fr.siovision.voyages.domain.model.Document;
import fr.siovision.voyages.infrastructure.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/me/documents")
@RequiredArgsConstructor
public class DocumentUploadController {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final DocumentUploadService documentUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(
            @RequestParam("documentTypeId") Long documentTypeId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        final String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (!ALLOWED_MIME_TYPES.contains(mime)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File type not allowed. Accepted: PDF, JPEG, PNG, WebP");
        }

        final String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        final byte[] bytes = file.getBytes();

        final Document newDoc = documentUploadService.uploadAndReplace(documentTypeId, bytes, originalName, mime);
        return new UploadResponse(newDoc.getPublicId().toString(), newDoc.getDocumentStatus().name());
    }
}
