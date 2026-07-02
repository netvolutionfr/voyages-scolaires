package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.GdprExportService;
import fr.siovision.voyages.infrastructure.dto.gdpr.GdprExportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class GdprController {

    private final GdprExportService gdprExportService;

    @GetMapping("/data-export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GdprExportResponse> exportMyData() {
        GdprExportResponse export = gdprExportService.export();
        String filename = "voyages-export-" + export.profile().publicId() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(export);
    }
}
