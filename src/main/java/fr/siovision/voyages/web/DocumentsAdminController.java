package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.DocumentsAdminService;
import fr.siovision.voyages.infrastructure.dto.DocumentsAdminDTO;
import fr.siovision.voyages.infrastructure.dto.PreviewUrlDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class DocumentsAdminController {

    private final DocumentsAdminService service;

    public DocumentsAdminController(DocumentsAdminService service) {
        this.service = service;
    }

    @GetMapping("/{userId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canViewUserDocumentsForTrip(#userId, #tripId)")
    public DocumentsAdminDTO getUserDocumentsForTrip(
            @PathVariable UUID userId,
            @RequestParam Long tripId
    ) {
        return service.getUserDocumentsForTrip(userId, tripId);
    }

    @GetMapping("/documents/{docId}/preview-url")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canPreviewDocument(#docId)")
    public PreviewUrlDTO getPreviewUrl(@PathVariable String docId) {
        return service.getPreviewUrl(docId);
    }
}