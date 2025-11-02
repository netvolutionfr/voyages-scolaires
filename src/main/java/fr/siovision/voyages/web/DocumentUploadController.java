package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.DocumentUploadService;
import fr.siovision.voyages.domain.model.Document;
import fr.siovision.voyages.infrastructure.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/me/documents")
@RequiredArgsConstructor
public class DocumentUploadController {
    private final DocumentUploadService documentUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(
            @RequestParam("documentTypeId") Long documentTypeId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        final String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        final String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        final byte[] bytes = file.getBytes();

        final Document newDoc = documentUploadService.uploadAndReplace(documentTypeId, bytes, originalName, mime);
        return new UploadResponse(newDoc.getPublicId().toString(), newDoc.getDocumentStatus().name());
    }
}