package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.ImportService;
import fr.siovision.voyages.infrastructure.dto.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("Received file for import: {}", file.getOriginalFilename());
        try (var is = file.getInputStream()) {
            return importService.importCsv(is);
        } catch (Exception e) {
            throw new Exception("Failed to import CSV: " + e.getMessage(), e);
        }
    }
}