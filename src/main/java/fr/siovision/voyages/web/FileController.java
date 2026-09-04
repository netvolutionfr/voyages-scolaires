package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final long MAX_COVER_SIZE_BYTES = 12L * 1024 * 1024;

    private final FileService fileService;

    @GetMapping("/presign")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER') and @tripSecurity.canViewTrip(#tripId)")
    public Map<String, String> presign(
            @RequestParam Long tripId,
            @RequestParam String filename,
            @RequestParam(defaultValue = "image/jpeg") String contentType,
            @RequestParam long contentLength
    ) {
        if (!isAllowedForCover(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType non autorisé pour une couverture");
        }
        if (contentLength <= 0 || contentLength > MAX_COVER_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taille de couverture invalide (12 Mo maximum)");
        }
        String key = fileService.buildCoverKey(tripId, filename);
        return fileService.presignPut(key, contentType, contentLength);
    }

    private boolean isAllowedForCover(String ct) {
        return "image/jpeg".equals(ct) || "image/png".equals(ct) || "image/webp".equals(ct);
    }

}
