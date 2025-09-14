package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.VoyageService;
import fr.siovision.voyages.domain.model.Voyage;
import fr.siovision.voyages.infrastructure.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voyages")
public class VoyageController {
    @Autowired
    private VoyageService voyageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<String> createVoyage(@RequestBody VoyageUpsertRequest voyageRequest) {
        // Appeler le service pour créer un nouveau voyage
        Voyage saved = voyageService.createVoyage(voyageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId().toString());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<String> updateVoyage(@PathVariable Long id, @RequestBody VoyageUpsertRequest voyageRequest) {
        // Mettre à jour une section existante
        Voyage updatedVoyage = voyageService.updateVoyage(id, voyageRequest);
        return ResponseEntity.status(HttpStatus.OK).body(updatedVoyage.getId().toString());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<Page<VoyageDetailDTO>> list(
            @PageableDefault(size = 20, sort = "dateDepart", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<VoyageDetailDTO> voyages = voyageService.list(pageable);
        return ResponseEntity.ok(voyages);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @RequestMapping("/{id}")
    public ResponseEntity<VoyageDetailDTO> getVoyageById(@PathVariable Long id) {
        VoyageDetailDTO voyage = voyageService.getVoyageById(id);
        return ResponseEntity.ok(voyage);
    }
}
