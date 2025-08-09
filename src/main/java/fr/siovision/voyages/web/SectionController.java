package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.SectionService;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sections")
public class SectionController {
    @Autowired
    private SectionService sectionService;

    @GetMapping()
    public ResponseEntity<Page<SectionDTO>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "libelle", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<SectionDTO> sections = sectionService.list(q, pageable);
        return ResponseEntity.ok(sections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SectionDTO> getSectionById(@PathVariable Long id) {
        // Récupérer une section par son ID
        SectionDTO section = sectionService.getSectionById(id);
        if (section != null) {
            return ResponseEntity.ok(section);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping()
    public ResponseEntity<SectionDTO> createSection(@RequestBody SectionDTO sectionDTO) {
        // Créer une nouvelle section
        SectionDTO createdSection = sectionService.createSection(sectionDTO);
        return ResponseEntity.status(201).body(createdSection);
    }

    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<SectionDTO> updateSection(@PathVariable Long id, @RequestBody SectionDTO sectionDTO) {
        // Mettre à jour une section existante
        SectionDTO updatedSection = sectionService.updateSection(id, sectionDTO);
        return ResponseEntity.ok(updatedSection);
    }

    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        // Supprimer une section par son ID
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}
